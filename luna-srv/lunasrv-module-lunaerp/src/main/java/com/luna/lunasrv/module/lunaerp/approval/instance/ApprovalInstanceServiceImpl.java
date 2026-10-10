package com.luna.lunasrv.module.lunaerp.approval.instance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.approval.biz.ApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigDO;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigNodeDO;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigNodeMapper;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigService;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalNodeSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalProcessSnapshotDTO;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.*;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordDO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskDO;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskMapper;
import com.luna.lunasrv.module.lunaerp.common.notify.LunaNotifyHelper;
import com.luna.lunasrv.module.system.api.dept.DeptApi;
import com.luna.lunasrv.module.system.api.dept.dto.DeptRespDTO;
import com.luna.lunasrv.module.system.api.permission.PermissionApi;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.approval.enums.ApprovalErrorCodeConstants.*;

/**
 * 审批实例 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ApprovalInstanceServiceImpl implements ApprovalInstanceService {

    public static final String STATUS_APPROVING = "APPROVING";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Resource
    private ApprovalConfigService approvalConfigService;

    @Resource
    private ApprovalConfigNodeMapper approvalConfigNodeMapper;

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Resource
    private ApprovalTaskMapper approvalTaskMapper;

    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private PermissionApi permissionApi;

    @Resource
    private List<ApprovalBizHandler> bizHandlers;

    private Map<String, ApprovalBizHandler> handlerMap;

    @PostConstruct
    public void init() {
        if (CollUtil.isNotEmpty(bizHandlers)) {
            handlerMap = CollectionUtils.convertMap(bizHandlers, ApprovalBizHandler::getBizType);
        } else {
            handlerMap = Collections.emptyMap();
        }
    }

    private ApprovalBizHandler getHandler(String bizType) {
        ApprovalBizHandler handler = handlerMap.get(bizType);
        if (handler == null) {
            throw exception(APPROVAL_CONFIG_NOT_ENABLED);
        }
        return handler;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitInstance(ApprovalInstanceSubmitReqVO submitReqVO) {
        // 0. 校验发起人权限（未登录不允许发起）
        Long submitterId = SecurityFrameworkUtils.getLoginUserId();
        if (submitterId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // 1. 业务单据处理器校验
        ApprovalBizHandler handler = getHandler(submitReqVO.getBizType());
        handler.validateCanSubmit(submitReqVO.getBizId());

        // 2. 校验单据类型是否有启用的审批流程
        ApprovalConfigDO config = approvalConfigService.getEnabledConfigByBizType(submitReqVO.getBizType());
        if (config == null) {
            throw exception(APPROVAL_CONFIG_NOT_ENABLED);
        }
        List<ApprovalConfigNodeDO> configNodes = approvalConfigNodeMapper.selectListByConfigId(config.getId());
        if (CollUtil.isEmpty(configNodes)) {
            throw exception(APPROVAL_CONFIG_NOT_ENABLED);
        }

        // 3. 提交人与单据信息
        AdminUserRespDTO submitter = adminUserApi != null ? adminUserApi.getUser(submitterId) : null;
        String submitterName = submitter != null ? submitter.getNickname() : "发起人";
        String bizNo = handler.getBizNo(submitReqVO.getBizId());

        // 4. 解析全流程节点审批人来源为快照
        List<ApprovalNodeSnapshotDTO> snapshotNodes = new ArrayList<>();
        for (ApprovalConfigNodeDO node : configNodes) {
            List<Long> approverUserIds = resolveApproverUserIds(node, submitterId, submitter);
            snapshotNodes.add(ApprovalNodeSnapshotDTO.builder()
                    .nodeConfigId(node.getId())
                    .sort(node.getSort())
                    .name(node.getName())
                    .nodeType(node.getNodeType())
                    .approverType(node.getApproverType())
                    .approverUserIds(approverUserIds)
                    .build());
        }

        ApprovalProcessSnapshotDTO snapshot = ApprovalProcessSnapshotDTO.builder()
                .configId(config.getId())
                .configName(config.getName())
                .bizType(config.getBizType())
                .nodes(snapshotNodes)
                .build();
        String snapshotJson = JsonUtils.toJsonString(snapshot);

        // 5. 创建审批实例（status=APPROVING, current_node_sort=1）
        ApprovalNodeSnapshotDTO firstNode = snapshotNodes.get(0);
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .bizType(submitReqVO.getBizType())
                .bizId(submitReqVO.getBizId())
                .bizNo(bizNo)
                .processSnapshot(snapshotJson)
                .status(STATUS_APPROVING)
                .submitterId(submitterId != null ? submitterId : 0L)
                .currentNodeSort(firstNode.getSort())
                .startTime(LocalDateTime.now())
                .build();
        approvalInstanceMapper.insert(instance);

        // 6. 创建首节点待办任务 (status=PENDING)
        List<Long> firstNodeApproverIds = firstNode.getApproverUserIds();
        if (CollUtil.isEmpty(firstNodeApproverIds)) {
            throw exception(APPROVAL_CONFIG_NOT_ENABLED, "审批首节点【" + firstNode.getName() + "】未找到审批人");
        }
        for (Long approverId : firstNodeApproverIds) {
            ApprovalTaskDO task = ApprovalTaskDO.builder()
                    .instanceId(instance.getId())
                    .nodeSort(firstNode.getSort())
                    .nodeName(firstNode.getName())
                    .nodeType(firstNode.getNodeType())
                    .approverId(approverId)
                    .status(ApprovalTaskDO.STATUS_PENDING)
                    .action(null)
                    .startTime(LocalDateTime.now())
                    .version(0)
                    .build();
            approvalTaskMapper.insert(task);
        }

        // 7. 写入 SUBMIT 操作记录
        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .instanceId(instance.getId())
                .taskId(null)
                .operatorId(submitterId != null ? submitterId : 0L)
                .action(ApprovalRecordDO.ACTION_SUBMIT)
                .comment(StrUtil.isNotBlank(submitReqVO.getComment()) ? submitReqVO.getComment() : "提交审批")
                .operateTime(LocalDateTime.now())
                .build();
        approvalRecordMapper.insert(record);

        // 8. 更新业务单据状态为 APPROVING
        handler.updateApprovalStatus(submitReqVO.getBizId(), STATUS_APPROVING);

        // 9. 站内信通知审批人待办
        LunaNotifyHelper notifyHelper = LunaNotifyHelper.getInstance();
        if (notifyHelper != null) {
            notifyHelper.sendApprovalTodoBatch(
                    firstNodeApproverIds,
                    submitReqVO.getBizType(),
                    instance.getId(),
                    bizNo,
                    handler.getBizTypeName(),
                    submitterName
            );
        }

        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawInstance(Long id, ApprovalInstanceWithdrawReqVO withdrawReqVO) {
        // 1. 校验实例存在
        ApprovalInstanceDO instance = approvalInstanceMapper.selectById(id);
        if (instance == null) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        // 2. 校验发起人权限（仅本人可撤回，非本人报 1_030_030_005）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !instance.getSubmitterId().equals(loginUserId)) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // 3. 校验状态（仅 APPROVING 态可撤回，否则报 1_030_030_004）
        if (!STATUS_APPROVING.equalsIgnoreCase(instance.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        // 4. 实例状态置为 CANCELLED
        instance.setStatus(STATUS_CANCELLED);
        instance.setEndTime(LocalDateTime.now());
        approvalInstanceMapper.updateById(instance);

        // 5. PENDING task 置 DONE + action=null
        List<ApprovalTaskDO> pendingTasks = approvalTaskMapper.selectPendingListByInstanceId(instance.getId());
        if (CollUtil.isNotEmpty(pendingTasks)) {
            LocalDateTime now = LocalDateTime.now();
            for (ApprovalTaskDO task : pendingTasks) {
                task.setStatus(ApprovalTaskDO.STATUS_DONE);
                task.setAction(null);
                task.setEndTime(now);
                approvalTaskMapper.updateById(task);
            }
        }

        // 6. 写 WITHDRAW 操作记录
        String reason = withdrawReqVO != null && StrUtil.isNotBlank(withdrawReqVO.getReason())
                ? withdrawReqVO.getReason() : "发起人撤回审批";
        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .instanceId(instance.getId())
                .taskId(null)
                .operatorId(loginUserId != null ? loginUserId : instance.getSubmitterId())
                .action(ApprovalRecordDO.ACTION_WITHDRAW)
                .comment(reason)
                .operateTime(LocalDateTime.now())
                .build();
        approvalRecordMapper.insert(record);

        // 7. 业务单据 approval_status 回 DRAFT
        ApprovalBizHandler handler = getHandler(instance.getBizType());
        handler.updateApprovalStatus(instance.getBizId(), "DRAFT");
    }

    @Override
    public PageResult<ApprovalInstanceMyRespVO> getMyPage(ApprovalInstanceMyPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        PageResult<ApprovalInstanceDO> pageResult = approvalInstanceMapper.selectMyPage(loginUserId, pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        AdminUserRespDTO user = adminUserApi != null ? adminUserApi.getUser(loginUserId) : null;
        String submitterName = user != null ? user.getNickname() : null;

        List<ApprovalInstanceMyRespVO> respList = new ArrayList<>();
        for (ApprovalInstanceDO inst : pageResult.getList()) {
            ApprovalBizHandler handler = getHandler(inst.getBizType());
            String currentNodeName = resolveCurrentNodeName(inst);

            ApprovalInstanceMyRespVO vo = ApprovalInstanceMyRespVO.builder()
                    .id(inst.getId())
                    .bizType(inst.getBizType())
                    .bizTypeName(handler != null ? handler.getBizTypeName() : inst.getBizType())
                    .bizId(inst.getBizId())
                    .bizNo(inst.getBizNo())
                    .status(inst.getStatus())
                    .submitterId(inst.getSubmitterId())
                    .submitterName(submitterName)
                    .currentNodeSort(inst.getCurrentNodeSort())
                    .currentNodeName(currentNodeName)
                    .startTime(inst.getStartTime())
                    .endTime(inst.getEndTime())
                    .build();
            respList.add(vo);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    private String resolveCurrentNodeName(ApprovalInstanceDO inst) {
        if ("APPROVED".equalsIgnoreCase(inst.getStatus())) {
            return "已通过";
        }
        if ("REJECTED".equalsIgnoreCase(inst.getStatus())) {
            return "已驳回";
        }
        if ("CANCELLED".equalsIgnoreCase(inst.getStatus())) {
            return "已撤回";
        }
        if (StrUtil.isNotBlank(inst.getProcessSnapshot())) {
            try {
                ApprovalProcessSnapshotDTO snapshot = JsonUtils.parseObject(inst.getProcessSnapshot(), ApprovalProcessSnapshotDTO.class);
                if (snapshot != null && CollUtil.isNotEmpty(snapshot.getNodes())) {
                    for (ApprovalNodeSnapshotDTO node : snapshot.getNodes()) {
                        if (Objects.equals(node.getSort(), inst.getCurrentNodeSort())) {
                            return node.getName();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "审批中";
    }

    @Override
    public ApprovalInstanceRespVO getInstance(Long id) {
        ApprovalInstanceDO instance = approvalInstanceMapper.selectById(id);
        if (instance == null) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        List<ApprovalTaskDO> tasks = approvalTaskMapper.selectListByInstanceId(instance.getId());

        // 越权过滤：对非 submitter 且非任一 task approver 的用户返回 404 (APPROVAL_INSTANCE_NOT_EXISTS)
        boolean isSubmitter = Objects.equals(instance.getSubmitterId(), loginUserId);
        boolean isApprover = tasks.stream().anyMatch(t -> Objects.equals(t.getApproverId(), loginUserId));
        if (!isSubmitter && !isApprover) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        return buildInstanceRespVO(instance, tasks);
    }

    @Override
    public ApprovalInstanceRespVO getLatestInstanceByBiz(String bizType, Long bizId) {
        ApprovalInstanceDO instance = approvalInstanceMapper.selectLatestByBiz(bizType, bizId);
        if (instance == null) {
            return null;
        }
        List<ApprovalTaskDO> tasks = approvalTaskMapper.selectListByInstanceId(instance.getId());
        return buildInstanceRespVO(instance, tasks);
    }

    private ApprovalInstanceRespVO buildInstanceRespVO(ApprovalInstanceDO instance) {
        return buildInstanceRespVO(instance, null);
    }

    private ApprovalInstanceRespVO buildInstanceRespVO(ApprovalInstanceDO instance, List<ApprovalTaskDO> tasks) {
        if (tasks == null) {
            tasks = approvalTaskMapper.selectListByInstanceId(instance.getId());
        }
        List<ApprovalRecordDO> records = approvalRecordMapper.selectListByInstanceId(instance.getId());

        // 解析快照节点
        ApprovalProcessSnapshotDTO snapshot = null;
        if (StrUtil.isNotBlank(instance.getProcessSnapshot())) {
            try {
                snapshot = JsonUtils.parseObject(instance.getProcessSnapshot(), ApprovalProcessSnapshotDTO.class);
            } catch (Exception ignored) {
            }
        }

        // 收集所有需要回显昵称的用户ID
        Set<Long> userIds = new HashSet<>();
        if (instance.getSubmitterId() != null) {
            userIds.add(instance.getSubmitterId());
        }
        for (ApprovalTaskDO task : tasks) {
            if (task.getApproverId() != null) {
                userIds.add(task.getApproverId());
            }
        }
        for (ApprovalRecordDO record : records) {
            if (record.getOperatorId() != null) {
                userIds.add(record.getOperatorId());
            }
            if (record.getTransferToId() != null) {
                userIds.add(record.getTransferToId());
            }
        }
        if (snapshot != null && CollUtil.isNotEmpty(snapshot.getNodes())) {
            for (ApprovalNodeSnapshotDTO node : snapshot.getNodes()) {
                if (CollUtil.isNotEmpty(node.getApproverUserIds())) {
                    userIds.addAll(node.getApproverUserIds());
                }
            }
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi != null && CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();

        ApprovalInstanceRespVO respVO = BeanUtils.toBean(instance, ApprovalInstanceRespVO.class);
        ApprovalBizHandler handler = getHandler(instance.getBizType());
        if (handler != null) {
            respVO.setBizTypeName(handler.getBizTypeName());
        } else {
            respVO.setBizTypeName(instance.getBizType());
        }

        if (instance.getSubmitterId() != null && userMap.containsKey(instance.getSubmitterId())) {
            respVO.setSubmitterName(userMap.get(instance.getSubmitterId()).getNickname());
        }

        // 计算节点进度列表 (nodeProgress)
        List<ApprovalNodeProgressRespVO> nodeProgressList = new ArrayList<>();
        if (snapshot != null && CollUtil.isNotEmpty(snapshot.getNodes())) {
            int currentNodeSort = instance.getCurrentNodeSort() != null ? instance.getCurrentNodeSort() : 1;
            String status = instance.getStatus() != null ? instance.getStatus() : STATUS_APPROVING;

            for (ApprovalNodeSnapshotDTO node : snapshot.getNodes()) {
                int sort = node.getSort() != null ? node.getSort() : 0;
                String nodeStatus;
                String statusDesc;

                if ("APPROVED".equalsIgnoreCase(status)) {
                    nodeStatus = "PASSED";
                    statusDesc = "已通过";
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    if (sort < currentNodeSort) {
                        nodeStatus = "PASSED";
                        statusDesc = "已通过";
                    } else if (sort == currentNodeSort) {
                        nodeStatus = "REJECTED";
                        statusDesc = "已驳回";
                    } else {
                        nodeStatus = "PENDING";
                        statusDesc = "未到达";
                    }
                } else if ("CANCELLED".equalsIgnoreCase(status)) {
                    if (sort < currentNodeSort) {
                        nodeStatus = "PASSED";
                        statusDesc = "已通过";
                    } else {
                        nodeStatus = "CANCELLED";
                        statusDesc = "已撤回";
                    }
                } else {
                    // APPROVING
                    if (sort < currentNodeSort) {
                        nodeStatus = "PASSED";
                        statusDesc = "已通过";
                    } else if (sort == currentNodeSort) {
                        nodeStatus = "CURRENT";
                        statusDesc = "审批中";
                    } else {
                        nodeStatus = "PENDING";
                        statusDesc = "待审批";
                    }
                }

                List<String> approverNames = new ArrayList<>();
                if (CollUtil.isNotEmpty(node.getApproverUserIds())) {
                    for (Long uid : node.getApproverUserIds()) {
                        AdminUserRespDTO u = userMap.get(uid);
                        if (u != null && StrUtil.isNotBlank(u.getNickname())) {
                            approverNames.add(u.getNickname());
                        }
                    }
                }

                ApprovalNodeProgressRespVO nodeVO = ApprovalNodeProgressRespVO.builder()
                        .sort(sort)
                        .name(node.getName())
                        .nodeType(node.getNodeType())
                        .approverType(node.getApproverType())
                        .approverUserIds(node.getApproverUserIds())
                        .approverNames(approverNames)
                        .status(nodeStatus)
                        .statusDesc(statusDesc)
                        .build();
                nodeProgressList.add(nodeVO);
            }
        }
        respVO.setNodeProgress(nodeProgressList);

        // tasks
        List<ApprovalTaskRespVO> taskRespList = new ArrayList<>();
        for (ApprovalTaskDO task : tasks) {
            ApprovalTaskRespVO taskVO = BeanUtils.toBean(task, ApprovalTaskRespVO.class);
            if (task.getApproverId() != null && userMap.containsKey(task.getApproverId())) {
                taskVO.setApproverName(userMap.get(task.getApproverId()).getNickname());
            }
            taskRespList.add(taskVO);
        }
        respVO.setTasks(taskRespList);

        // records
        List<ApprovalRecordRespVO> recordRespList = new ArrayList<>();
        for (ApprovalRecordDO record : records) {
            ApprovalRecordRespVO recordVO = BeanUtils.toBean(record, ApprovalRecordRespVO.class);
            if (record.getOperatorId() != null && userMap.containsKey(record.getOperatorId())) {
                recordVO.setOperatorName(userMap.get(record.getOperatorId()).getNickname());
            }
            if (record.getTransferToId() != null && userMap.containsKey(record.getTransferToId())) {
                recordVO.setTransferToName(userMap.get(record.getTransferToId()).getNickname());
            }
            recordVO.setActionName(formatRecordActionName(record.getAction()));
            recordRespList.add(recordVO);
        }
        respVO.setRecords(recordRespList);

        return respVO;
    }

    private String formatRecordActionName(String action) {
        if ("APPROVE".equalsIgnoreCase(action)) {
            return "通过";
        } else if ("REJECT".equalsIgnoreCase(action)) {
            return "驳回";
        } else if ("TRANSFER".equalsIgnoreCase(action)) {
            return "转交";
        } else if ("SUBMIT".equalsIgnoreCase(action)) {
            return "提交";
        } else if ("WITHDRAW".equalsIgnoreCase(action)) {
            return "撤回";
        }
        return action;
    }

    private List<Long> resolveApproverUserIds(ApprovalConfigNodeDO node, Long submitterId, AdminUserRespDTO submitter) {
        String approverType = node.getApproverType();
        if ("USER".equalsIgnoreCase(approverType)) {
            return parseLongList(node.getApproverIds());
        } else if ("POST".equalsIgnoreCase(approverType)) {
            List<Long> postIds = parseLongList(node.getApproverIds());
            if (CollUtil.isEmpty(postIds) || adminUserApi == null) {
                return Collections.emptyList();
            }
            List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(postIds);
            return CollectionUtils.convertList(users, AdminUserRespDTO::getId);
        } else if ("ROLE".equalsIgnoreCase(approverType)) {
            List<Long> roleIds = parseLongList(node.getApproverIds());
            if (CollUtil.isEmpty(roleIds) || permissionApi == null) {
                return Collections.emptyList();
            }
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(roleIds);
            return userIds != null ? new ArrayList<>(userIds) : Collections.emptyList();
        } else if ("DEPT_LEADER".equalsIgnoreCase(approverType)) {
            if (submitter != null && submitter.getDeptId() != null && deptApi != null) {
                DeptRespDTO dept = deptApi.getDept(submitter.getDeptId());
                if (dept != null && dept.getLeaderUserId() != null) {
                    return Collections.singletonList(dept.getLeaderUserId());
                }
            }
            return Collections.emptyList();
        } else if ("SUBMITTER_LEADER".equalsIgnoreCase(approverType)) {
            if (submitter != null && submitter.getDeptId() != null && deptApi != null) {
                DeptRespDTO dept = deptApi.getDept(submitter.getDeptId());
                if (dept != null) {
                    // 若当前提交人不是部门负责人且负责人存在，即为直属上级
                    if (dept.getLeaderUserId() != null && !dept.getLeaderUserId().equals(submitterId)) {
                        return Collections.singletonList(dept.getLeaderUserId());
                    }
                    // 提交人自身是部门负责人，向上层父部门寻找负责人
                    List<DeptRespDTO> parents = deptApi.getParentDeptList(dept.getId());
                    if (CollUtil.isNotEmpty(parents)) {
                        for (DeptRespDTO parent : parents) {
                            if (parent.getLeaderUserId() != null && !parent.getLeaderUserId().equals(submitterId)) {
                                return Collections.singletonList(parent.getLeaderUserId());
                            }
                        }
                    }
                    if (dept.getLeaderUserId() != null) {
                        return Collections.singletonList(dept.getLeaderUserId());
                    }
                }
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<Long> parseLongList(String str) {
        if (StrUtil.isBlank(str)) {
            return Collections.emptyList();
        }
        String trimmed = str.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return JsonUtils.parseArray(trimmed, Long.class);
            } catch (Exception ignored) {
                // fall through
            }
        }
        String[] parts = trimmed.split("[,;\\s]+");
        List<Long> result = new ArrayList<>();
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                try {
                    result.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

}

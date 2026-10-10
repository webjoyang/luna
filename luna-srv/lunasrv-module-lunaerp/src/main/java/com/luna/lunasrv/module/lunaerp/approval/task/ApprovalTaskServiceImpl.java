package com.luna.lunasrv.module.lunaerp.approval.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.approval.biz.ApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.event.ApprovalCompletedEvent;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceMapper;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalNodeSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalProcessSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordDO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.*;
import com.luna.lunasrv.module.lunaerp.common.notify.LunaNotifyHelper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.approval.enums.ApprovalErrorCodeConstants.*;

/**
 * 审批任务 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    @Resource
    private ApprovalTaskMapper approvalTaskMapper;

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

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
        if (handlerMap == null) {
            return null;
        }
        return handlerMap.get(bizType);
    }

    @Override
    public PageResult<ApprovalTaskTodoRespVO> getTodoPage(ApprovalTaskTodoPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        PageResult<ApprovalTaskDO> taskPage = approvalTaskMapper.selectTodoPage(loginUserId, pageReqVO);
        if (CollUtil.isEmpty(taskPage.getList())) {
            return new PageResult<>(Collections.emptyList(), taskPage.getTotal());
        }

        // 批量查询审批实例
        List<Long> instanceIds = CollectionUtils.convertList(taskPage.getList(), ApprovalTaskDO::getInstanceId);
        List<ApprovalInstanceDO> instances = approvalInstanceMapper.selectBatchIds(instanceIds);
        Map<Long, ApprovalInstanceDO> instanceMap = CollectionUtils.convertMap(instances, ApprovalInstanceDO::getId);

        // 收集用户编号查询昵称
        Set<Long> userIds = new HashSet<>();
        userIds.add(loginUserId);
        for (ApprovalInstanceDO inst : instances) {
            if (inst != null && inst.getSubmitterId() != null) {
                userIds.add(inst.getSubmitterId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = (adminUserApi != null && CollUtil.isNotEmpty(userIds))
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();

        LocalDateTime now = LocalDateTime.now();
        List<ApprovalTaskTodoRespVO> respList = new ArrayList<>();
        for (ApprovalTaskDO task : taskPage.getList()) {
            ApprovalInstanceDO inst = instanceMap.get(task.getInstanceId());
            ApprovalBizHandler handler = inst != null ? getHandler(inst.getBizType()) : null;

            LocalDateTime taskStart = task.getStartTime() != null ? task.getStartTime() : now;
            Duration duration = Duration.between(taskStart, now);
            long timeoutMillis = Math.max(0, duration.toMillis());
            String timeoutDesc = formatDuration(duration);

            AdminUserRespDTO approver = userMap.get(task.getApproverId());
            AdminUserRespDTO submitter = (inst != null && inst.getSubmitterId() != null)
                    ? userMap.get(inst.getSubmitterId()) : null;

            ApprovalTaskTodoRespVO vo = ApprovalTaskTodoRespVO.builder()
                    .id(task.getId())
                    .instanceId(task.getInstanceId())
                    .nodeSort(task.getNodeSort())
                    .nodeName(task.getNodeName())
                    .nodeType(task.getNodeType())
                    .approverId(task.getApproverId())
                    .approverName(approver != null ? approver.getNickname() : null)
                    .bizType(inst != null ? inst.getBizType() : null)
                    .bizTypeName(handler != null ? handler.getBizTypeName() : (inst != null ? inst.getBizType() : ""))
                    .bizId(inst != null ? inst.getBizId() : null)
                    .bizNo(inst != null ? inst.getBizNo() : null)
                    .submitterId(inst != null ? inst.getSubmitterId() : null)
                    .submitterName(submitter != null ? submitter.getNickname() : null)
                    .startTime(task.getStartTime())
                    .instanceStartTime(inst != null ? inst.getStartTime() : null)
                    .timeoutDuration(timeoutMillis)
                    .timeoutDurationDesc(timeoutDesc)
                    .status(task.getStatus())
                    .build();
            respList.add(vo);
        }

        return new PageResult<>(respList, taskPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long id, ApprovalTaskApproveReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        ApprovalTaskDO task = approvalTaskMapper.selectById(id);
        if (task == null) {
            throw exception(APPROVAL_TASK_NOT_EXISTS);
        }

        ApprovalInstanceDO instance = approvalInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        // 终态守卫：实例非 APPROVING 时不允许任何 task 操作
        if (!ApprovalInstanceDO.STATUS_APPROVING.equalsIgnoreCase(instance.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        // 权限守卫：仅当前 task 的 approver_id=本人可操作
        if (!Objects.equals(task.getApproverId(), loginUserId)) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // 任务状态守卫：task 必须 PENDING
        if (!ApprovalTaskDO.STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 更新当前 task：置 DONE + action=DONE（乐观锁版本防并发）
        task.setStatus(ApprovalTaskDO.STATUS_DONE);
        task.setAction(ApprovalTaskDO.ACTION_DONE);
        task.setEndTime(now);
        int rows = approvalTaskMapper.updateById(task);
        if (rows == 0) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "任务状态已被修改或并发冲突，请刷新后重试");
        }

        // 2. 写入 APPROVE 操作记录
        String comment = (reqVO != null && StrUtil.isNotBlank(reqVO.getComment())) ? reqVO.getComment() : "同意";
        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .instanceId(instance.getId())
                .taskId(task.getId())
                .operatorId(loginUserId)
                .action(ApprovalRecordDO.ACTION_APPROVE)
                .comment(comment)
                .operateTime(now)
                .build();
        approvalRecordMapper.insert(record);

        // 3. 解析流程快照与节点推进
        ApprovalProcessSnapshotDTO snapshot = JsonUtils.parseObject(instance.getProcessSnapshot(), ApprovalProcessSnapshotDTO.class);
        ApprovalNodeSnapshotDTO currentNode = null;
        if (snapshot != null && CollUtil.isNotEmpty(snapshot.getNodes())) {
            currentNode = snapshot.getNodes().stream()
                    .filter(n -> Objects.equals(n.getSort(), task.getNodeSort()))
                    .findFirst().orElse(null);
        }
        String nodeType = currentNode != null ? currentNode.getNodeType() : task.getNodeType();

        boolean nodePassed = false;
        if ("SEQUENTIAL".equalsIgnoreCase(nodeType)) {
            // 逐级：单人通过即本节点通过
            nodePassed = true;
        } else if ("OR_SIGN".equalsIgnoreCase(nodeType)) {
            // 或签：任一通过即过，同节点其他 PENDING task 置 DONE+action=null（自动关闭）
            nodePassed = true;
            List<ApprovalTaskDO> sameNodeTasks = approvalTaskMapper.selectListByInstanceIdAndNodeSort(instance.getId(), task.getNodeSort());
            for (ApprovalTaskDO other : sameNodeTasks) {
                if (!other.getId().equals(task.getId()) && ApprovalTaskDO.STATUS_PENDING.equalsIgnoreCase(other.getStatus())) {
                    other.setStatus(ApprovalTaskDO.STATUS_DONE);
                    other.setAction(null);
                    other.setEndTime(now);
                    approvalTaskMapper.updateById(other);
                }
            }
        } else if ("AND_SIGN".equalsIgnoreCase(nodeType)) {
            // 会签：全部通过才过
            List<ApprovalTaskDO> sameNodeTasks = approvalTaskMapper.selectListByInstanceIdAndNodeSort(instance.getId(), task.getNodeSort());
            boolean allDone = sameNodeTasks.stream().allMatch(t ->
                    ApprovalTaskDO.STATUS_DONE.equalsIgnoreCase(t.getStatus()) && ApprovalTaskDO.ACTION_DONE.equalsIgnoreCase(t.getAction()));
            if (allDone) {
                nodePassed = true;
            }
        } else {
            // 兜底逐级
            nodePassed = true;
        }

        // 4. 节点通过后的处理
        if (nodePassed) {
            ApprovalNodeSnapshotDTO nextNode = null;
            if (snapshot != null && CollUtil.isNotEmpty(snapshot.getNodes())) {
                nextNode = snapshot.getNodes().stream()
                        .filter(n -> n.getSort() > task.getNodeSort())
                        .min(Comparator.comparingInt(ApprovalNodeSnapshotDTO::getSort))
                        .orElse(null);
            }

            ApprovalBizHandler handler = getHandler(instance.getBizType());

            if (nextNode != null) {
                // 有下一节点：创建下一节点 task(s) 并通知
                instance.setCurrentNodeSort(nextNode.getSort());
                approvalInstanceMapper.updateById(instance);

                List<Long> nextApproverIds = nextNode.getApproverUserIds();
                if (CollUtil.isNotEmpty(nextApproverIds)) {
                    for (Long nextApproverId : nextApproverIds) {
                        ApprovalTaskDO nextTask = ApprovalTaskDO.builder()
                                .instanceId(instance.getId())
                                .nodeSort(nextNode.getSort())
                                .nodeName(nextNode.getName())
                                .nodeType(nextNode.getNodeType())
                                .approverId(nextApproverId)
                                .status(ApprovalTaskDO.STATUS_PENDING)
                                .action(null)
                                .startTime(now)
                                .version(0)
                                .build();
                        approvalTaskMapper.insert(nextTask);
                    }

                    LunaNotifyHelper notifyHelper = LunaNotifyHelper.getInstance();
                    if (notifyHelper != null) {
                        AdminUserRespDTO submitter = adminUserApi != null ? adminUserApi.getUser(instance.getSubmitterId()) : null;
                        String submitterName = submitter != null ? submitter.getNickname() : "发起人";
                        notifyHelper.sendApprovalTodoBatch(
                                nextApproverIds,
                                instance.getBizType(),
                                instance.getId(),
                                instance.getBizNo(),
                                handler != null ? handler.getBizTypeName() : "业务单据",
                                submitterName
                        );
                    }
                }
            } else {
                // 无下一节点：全流程通过，instance.status=APPROVED
                instance.setStatus(ApprovalInstanceDO.STATUS_APPROVED);
                instance.setCurrentNodeSort(-1);
                instance.setEndTime(now);
                approvalInstanceMapper.updateById(instance);

                if (handler != null) {
                    handler.updateApprovalStatus(instance.getBizId(), ApprovalInstanceDO.STATUS_APPROVED);
                }

                LunaNotifyHelper notifyHelper = LunaNotifyHelper.getInstance();
                if (notifyHelper != null) {
                    notifyHelper.sendApprovalApproved(
                            instance.getSubmitterId(),
                            instance.getBizType(),
                            instance.getId(),
                            instance.getBizNo(),
                            handler != null ? handler.getBizTypeName() : "业务单据"
                    );
                }

                // 发布审批完成事件（BEFORE_COMMIT 同事务副作用，见 Issue 07）
                if (applicationEventPublisher != null) {
                    applicationEventPublisher.publishEvent(new ApprovalCompletedEvent(this, instance.getBizType(), instance.getBizId()));
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long id, ApprovalTaskRejectReqVO reqVO) {
        // 1. comment 必填校验（1_030_030_006）
        if (reqVO == null || StrUtil.isBlank(reqVO.getComment())) {
            throw exception(APPROVAL_REJECT_COMMENT_EMPTY);
        }

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        ApprovalTaskDO task = approvalTaskMapper.selectById(id);
        if (task == null) {
            throw exception(APPROVAL_TASK_NOT_EXISTS);
        }

        ApprovalInstanceDO instance = approvalInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        // 终态守卫：instance 非 APPROVING 时不允许任何 task 操作
        if (!ApprovalInstanceDO.STATUS_APPROVING.equalsIgnoreCase(instance.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        // 权限守卫：仅当前 task 的 approver_id=本人可操作
        if (!Objects.equals(task.getApproverId(), loginUserId)) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // 任务状态守卫：task 必须 PENDING
        if (!ApprovalTaskDO.STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 当前 task 置 DONE + REJECTED
        task.setStatus(ApprovalTaskDO.STATUS_DONE);
        task.setAction(ApprovalTaskDO.ACTION_REJECTED);
        task.setEndTime(now);
        int rows = approvalTaskMapper.updateById(task);
        if (rows == 0) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "任务状态已被修改或并发冲突，请刷新后重试");
        }

        // 3. 同节点其他 PENDING task 置 DONE + action=null（自动关闭）
        List<ApprovalTaskDO> sameNodeTasks = approvalTaskMapper.selectListByInstanceIdAndNodeSort(instance.getId(), task.getNodeSort());
        for (ApprovalTaskDO other : sameNodeTasks) {
            if (!other.getId().equals(task.getId()) && ApprovalTaskDO.STATUS_PENDING.equalsIgnoreCase(other.getStatus())) {
                other.setStatus(ApprovalTaskDO.STATUS_DONE);
                other.setAction(null);
                other.setEndTime(now);
                approvalTaskMapper.updateById(other);
            }
        }

        // 4. instance 置 REJECTED，流程终止
        instance.setStatus(ApprovalInstanceDO.STATUS_REJECTED);
        instance.setCurrentNodeSort(-1);
        instance.setEndTime(now);
        approvalInstanceMapper.updateById(instance);

        // 5. 业务单据 approval_status 置 REJECTED（回到可编辑）
        ApprovalBizHandler handler = getHandler(instance.getBizType());
        if (handler != null) {
            handler.updateApprovalStatus(instance.getBizId(), ApprovalInstanceDO.STATUS_REJECTED);
        }

        // 6. 写入 REJECT 记录
        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .instanceId(instance.getId())
                .taskId(task.getId())
                .operatorId(loginUserId)
                .action(ApprovalRecordDO.ACTION_REJECT)
                .comment(reqVO.getComment())
                .operateTime(now)
                .build();
        approvalRecordMapper.insert(record);

        // 7. 通知发起人审批被驳回
        LunaNotifyHelper notifyHelper = LunaNotifyHelper.getInstance();
        if (notifyHelper != null) {
            notifyHelper.sendApprovalRejected(
                    instance.getSubmitterId(),
                    instance.getBizType(),
                    instance.getId(),
                    instance.getBizNo(),
                    handler != null ? handler.getBizTypeName() : "业务单据",
                    reqVO.getComment()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(Long id, ApprovalTaskTransferReqVO reqVO) {
        if (reqVO == null || reqVO.getToUserId() == null) {
            throw exception(BAD_REQUEST, "转交接收人不能为空");
        }

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // toUserId=本人报 1_030_030_009
        if (Objects.equals(reqVO.getToUserId(), loginUserId)) {
            throw exception(APPROVAL_TRANSFER_TO_SELF_NOT_ALLOWED);
        }

        ApprovalTaskDO task = approvalTaskMapper.selectById(id);
        if (task == null) {
            throw exception(APPROVAL_TASK_NOT_EXISTS);
        }

        ApprovalInstanceDO instance = approvalInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw exception(APPROVAL_INSTANCE_NOT_EXISTS);
        }

        // 终态守卫：instance 非 APPROVING 时不允许任何 task 操作
        if (!ApprovalInstanceDO.STATUS_APPROVING.equalsIgnoreCase(instance.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        // 权限守卫：仅当前 task 的 approver_id=本人可操作
        if (!Objects.equals(task.getApproverId(), loginUserId)) {
            throw exception(APPROVAL_NO_PERMISSION);
        }

        // 任务状态守卫：task 必须 PENDING
        if (!ApprovalTaskDO.STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 当前 task 置 DONE + TRANSFERRED
        task.setStatus(ApprovalTaskDO.STATUS_DONE);
        task.setAction(ApprovalTaskDO.ACTION_TRANSFERRED);
        task.setEndTime(now);
        int rows = approvalTaskMapper.updateById(task);
        if (rows == 0) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "任务状态已被修改或并发冲突，请刷新后重试");
        }

        // 2. 新建 task 给接收人（同 node_sort、PENDING）
        ApprovalTaskDO newTask = ApprovalTaskDO.builder()
                .instanceId(instance.getId())
                .nodeSort(task.getNodeSort())
                .nodeName(task.getNodeName())
                .nodeType(task.getNodeType())
                .approverId(reqVO.getToUserId())
                .status(ApprovalTaskDO.STATUS_PENDING)
                .action(null)
                .startTime(now)
                .version(0)
                .build();
        approvalTaskMapper.insert(newTask);

        // 3. 写入 TRANSFER 记录（含 transfer_to_id）
        String comment = StrUtil.isNotBlank(reqVO.getComment()) ? reqVO.getComment() : "转交处理";
        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .instanceId(instance.getId())
                .taskId(task.getId())
                .operatorId(loginUserId)
                .action(ApprovalRecordDO.ACTION_TRANSFER)
                .comment(comment)
                .transferToId(reqVO.getToUserId())
                .operateTime(now)
                .build();
        approvalRecordMapper.insert(record);

        // 4. 通知接收人待办任务
        LunaNotifyHelper notifyHelper = LunaNotifyHelper.getInstance();
        if (notifyHelper != null) {
            ApprovalBizHandler handler = getHandler(instance.getBizType());
            AdminUserRespDTO submitter = adminUserApi != null ? adminUserApi.getUser(instance.getSubmitterId()) : null;
            String submitterName = submitter != null ? submitter.getNickname() : "发起人";
            notifyHelper.sendApprovalTodo(
                    reqVO.getToUserId(),
                    instance.getBizType(),
                    instance.getId(),
                    instance.getBizNo(),
                    handler != null ? handler.getBizTypeName() : "业务单据",
                    submitterName
            );
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0分钟";
        }
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return days + "天" + hours + "小时";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分";
        } else {
            return Math.max(1, minutes) + "分钟";
        }
    }

}

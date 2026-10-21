package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStatusCountsVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.enums.WorkshopH5ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 车间移动 H5 接入层编排服务实现类
 *
 * 规范守卫：
 * 1. 强制数据范围：基于当前登录用户 ID 硬过滤，严禁信任或采用客户端传入的 workerId；
 * 2. 成功出参使用 H5 精简 VO，严格过滤成本、单价、内部备注等非现场信息；
 * 3. 读链路批量抽取快照（防 N+1，batch* 前缀方法封装），写链路一律委托生产域核心 Service。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class WorkshopH5ServiceImpl implements WorkshopH5Service {

    private static final List<String> DEFAULT_ACTIVE_STATUSES = List.of("PENDING", "IN_PROGRESS", "HALTED");

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private WeavingTicketMapper weavingTicketMapper;

    @Resource
    private WeavingTicketService weavingTicketService;

    @Override
    public PageResult<WorkshopMyTaskRespVO> getMyTasksPage(WorkshopMyTasksPageReqVO reqVO) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 解析状态过滤条件（缺省默认 PENDING, IN_PROGRESS, HALTED）
        List<String> statuses;
        if (StrUtil.isNotBlank(reqVO.getStatus())) {
            statuses = Arrays.stream(reqVO.getStatus().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
            if (statuses.isEmpty()) {
                statuses = DEFAULT_ACTIVE_STATUSES;
            }
        } else {
            statuses = DEFAULT_ACTIVE_STATUSES;
        }

        // 3. 硬过滤 worker_user_id = 登录人 分页查询机台任务
        LambdaQueryWrapperX<MachineTaskDO> taskQuery = new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getWorkerUserId, loginUserId)
                .in(MachineTaskDO::getStatus, statuses)
                .orderByDesc(MachineTaskDO::getId);
        PageResult<MachineTaskDO> pageResult = machineTaskMapper.selectPage(reqVO, taskQuery);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }

        // 4. 批量查询织造单快照（防 N+1，符合 coding-standards.md §2 batch* 命名规范）
        Set<Long> orderIds = pageResult.getList().stream()
                .map(MachineTaskDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = batchLoadWeavingOrders(orderIds);

        // 5. 批量查询关联布票各状态计数（防 N+1，仅投影 machineTaskId 与 status）
        List<Long> taskIds = pageResult.getList().stream()
                .map(MachineTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Map<String, Integer>> taskStatusCountsMap = batchLoadTicketStatusCounts(taskIds);

        // 6. 组装 H5 精简 VO
        List<WorkshopMyTaskRespVO> voList = pageResult.getList().stream().map(task -> {
            WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
            Map<String, Integer> counts = taskStatusCountsMap.getOrDefault(task.getId(), Collections.emptyMap());
            WorkshopTicketStatusCountsVO ticketCounts = WorkshopTicketStatusCountsVO.fromStatusCounts(counts);

            return WorkshopMyTaskRespVO.builder()
                    .id(task.getId())
                    .code(task.getCode())
                    .weavingOrderId(task.getWeavingOrderId())
                    .weavingOrderCode(order != null ? order.getCode() : null)
                    .productName(order != null ? order.getProductName() : null)
                    .productCode(order != null ? order.getProductCode() : null)
                    .loomId(task.getLoomId())
                    .loomNo(task.getLoomNo())
                    .workerUserId(task.getWorkerUserId())
                    .workerName(task.getWorkerName())
                    .plannedQty(task.getPlannedQty() != null ? task.getPlannedQty() : 0)
                    .actualQty(task.getActualQty() != null ? task.getActualQty() : 0)
                    .status(task.getStatus())
                    .plannedStart(task.getPlannedStart())
                    .plannedEnd(task.getPlannedEnd())
                    .ticketCounts(ticketCounts)
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public WorkshopTicketRespVO getTicketByCode(String code) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验参数
        if (StrUtil.isBlank(code)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（未删除）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, code.trim())
        );
        if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 4. 查询关联机台任务并进行挡车工权限与自助认领守卫
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null && task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }
        if (task == null && ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 查询关联织造单快照
        WeavingOrderDO order = ticket.getWeavingOrderId() != null
                ? weavingOrderMapper.selectById(ticket.getWeavingOrderId())
                : null;

        // 6. 确定挡车工归属与认领标志（任务 worker 为空或=登录人，claimable=true）
        Long workerUserId = task != null && task.getWorkerUserId() != null ? task.getWorkerUserId() : ticket.getWorkerUserId();
        String workerName = task != null && task.getWorkerName() != null ? task.getWorkerName() : ticket.getWorkerName();

        // 7. 组装并返回 H5 精简布票卡 VO
        return WorkshopTicketRespVO.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .weavingOrderId(ticket.getWeavingOrderId())
                .weavingOrderCode(order != null ? order.getCode() : null)
                .machineTaskId(ticket.getMachineTaskId())
                .machineTaskCode(task != null ? task.getCode() : null)
                .loomId(task != null ? task.getLoomId() : null)
                .loomNo(task != null ? task.getLoomNo() : ticket.getPlannedMachine())
                .plannedMachine(ticket.getPlannedMachine())
                .actualMachine(ticket.getActualMachine())
                .productName(order != null ? order.getProductName() : null)
                .productCode(order != null ? order.getProductCode() : null)
                .composition(order != null ? order.getComposition() : null)
                .gsm(order != null ? order.getGsm() : null)
                .width(order != null ? order.getWidth() : null)
                .plannedWeight(ticket.getPlannedWeight())
                .workerUserId(workerUserId)
                .workerName(workerName)
                .materialBatchNo(ticket.getMaterialBatchNo())
                .status(ticket.getStatus())
                .exceptionType(ticket.getExceptionType())
                .exceptionNote(ticket.getExceptionNote())
                .exceptionPhotos(ticket.getExceptionPhotos())
                .startedAt(ticket.getStartedAt())
                .completedAt(ticket.getCompletedAt())
                .claimable(Boolean.TRUE)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "车间移动H5", subType = "布票开工", bizNo = "{{#reqVO.code}}", success = "布票【{{#reqVO.code}}】开工成功")
    public void startTicket(WorkshopTicketStartReqVO reqVO) {
        // 1. 强制取登录人用户ID与昵称，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        String loginNickname = SecurityFrameworkUtils.getLoginUserNickname();

        // 2. 校验参数
        if (reqVO == null || StrUtil.isBlank(reqVO.getCode())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（非 VOID）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, reqVO.getCode().trim())
        );
        if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 4. 查询关联机台任务并进行挡车工权限与自助认领守卫
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null) {
            if (task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
                // 已派给其他挡车工，横向越权拦截
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
            }
            if (task.getWorkerUserId() == null) {
                // 未派工：自助认领，原子条件更新 WHERE worker_user_id IS NULL
                MachineTaskDO updateTask = new MachineTaskDO();
                updateTask.setWorkerUserId(loginUserId);
                updateTask.setWorkerName(loginNickname);
                int updated = machineTaskMapper.update(updateTask,
                        new LambdaUpdateWrapper<MachineTaskDO>()
                                .eq(MachineTaskDO::getId, task.getId())
                                .isNull(MachineTaskDO::getWorkerUserId)
                );
                if (updated == 0) {
                    // 并发落败：被其他工人抢先认领
                    throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
                }
            }
        } else if (ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 委托生产域核心 Service 执行开工，workerUserId/workerName 强制绑定当前登录人
        WeavingTicketStartReqVO startReqVO = WeavingTicketStartReqVO.builder()
                .id(ticket.getId())
                .actualMachine(StrUtil.isNotBlank(reqVO.getActualMachine()) ? reqVO.getActualMachine().trim() : null)
                .workerUserId(loginUserId)
                .workerName(loginNickname)
                .build();
        weavingTicketService.startTicket(startReqVO);
    }

    /**
     * 批量加载织造单快照（coding-standards.md §2 batch* 规范）
     */
    private Map<Long, WeavingOrderDO> batchLoadWeavingOrders(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        return weavingOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(WeavingOrderDO::getId, o -> o, (a, b) -> a));
    }

    /**
     * 批量查询机台任务的布票各状态计数（coding-standards.md §2 batch* 规范）
     */
    private Map<Long, Map<String, Integer>> batchLoadTicketStatusCounts(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<WeavingTicketDO> tickets = weavingTicketMapper.selectList(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .in(WeavingTicketDO::getMachineTaskId, taskIds)
                        .select(WeavingTicketDO::getMachineTaskId, WeavingTicketDO::getStatus)
        );
        Map<Long, Map<String, Integer>> resultMap = new HashMap<>();
        for (WeavingTicketDO ticket : tickets) {
            if (ticket.getMachineTaskId() == null || ticket.getStatus() == null) {
                continue;
            }
            resultMap
                    .computeIfAbsent(ticket.getMachineTaskId(), k -> new HashMap<>())
                    .merge(ticket.getStatus(), 1, Integer::sum);
        }
        return resultMap;
    }

}

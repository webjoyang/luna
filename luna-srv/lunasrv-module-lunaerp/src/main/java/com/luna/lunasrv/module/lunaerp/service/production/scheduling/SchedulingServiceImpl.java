package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.MACHINE_TASK_DATE_RANGE_INVALID;

/**
 * 生产管理 - 排产中心 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SchedulingServiceImpl implements SchedulingService {

    @Resource
    private LoomMapper loomMapper;

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Override
    public GanttRespVO getGanttData(GanttQueryReqVO reqVO) {
        validateGanttQuery(reqVO);

        // 1. 查询全部 ACTIVE 织机（含窗口内无任务的空行；INACTIVE 不返回；支持 loomIds 过滤）
        List<LoomDO> looms = loomMapper.selectList(new LambdaQueryWrapperX<LoomDO>()
                .eq(LoomDO::getStatus, "ACTIVE")
                .inIfPresent(LoomDO::getId, reqVO.getLoomIds())
                .orderByAsc(LoomDO::getId));
        if (CollUtil.isEmpty(looms)) {
            return new GanttRespVO(Collections.emptyList(), Collections.emptyList());
        }

        List<GanttLoomVO> loomVOs = looms.stream()
                .map(l -> new GanttLoomVO(l.getId(), l.getLoomNo(), l.getModel()))
                .collect(Collectors.toList());

        List<Long> activeLoomIds = looms.stream().map(LoomDO::getId).collect(Collectors.toList());

        // 2. 批量查询 ACTIVE 织机下所有占用态已排期任务，计算 conflict 标记（同源口径：存量互相重叠标 true，COMPLETED 不参与）
        List<MachineTaskDO> occupiedTasks = machineTaskMapper.selectOccupiedTasksByLoomIds(activeLoomIds);
        Set<Long> conflictTaskIds = SchedulingConflictUtils.findConflictTaskIds(occupiedTasks);

        // 3. 查询这些织机下符合状态条件且已排期的任务作为条带候选集
        List<MachineTaskDO> candidateTasks = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .in(MachineTaskDO::getLoomId, activeLoomIds)
                .inIfPresent(MachineTaskDO::getStatus, reqVO.getStatuses())
                .and(w -> w.isNotNull(MachineTaskDO::getPlannedStart).or().isNotNull(MachineTaskDO::getPlannedEnd))
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByAsc(MachineTaskDO::getId));

        // 4. 过滤与窗口 [start, end] 闭区间相交的条带（effStart <= windowEnd && effEnd >= windowStart）
        LocalDate windowStart = reqVO.getStart();
        LocalDate windowEnd = reqVO.getEnd();
        List<MachineTaskDO> intersectingTasks = candidateTasks.stream()
                .filter(t -> SchedulingConflictUtils.isIntervalOverlap(
                        t.getPlannedStart(), t.getPlannedEnd(), windowStart, windowEnd))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(intersectingTasks)) {
            return new GanttRespVO(loomVOs, Collections.emptyList());
        }

        // 5. 批量查询关联织造单快照（避免 N+1）
        Set<Long> orderIds = intersectingTasks.stream()
                .map(MachineTaskDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(orderIds)) {
            List<WeavingOrderDO> orders = weavingOrderMapper.selectBatchIds(orderIds);
            if (CollUtil.isNotEmpty(orders)) {
                orderMap = orders.stream().collect(Collectors.toMap(WeavingOrderDO::getId, Function.identity()));
            }
        }

        // 6. 组装条带 VO
        List<GanttBarVO> barVOs = new ArrayList<>(intersectingTasks.size());
        for (MachineTaskDO task : intersectingTasks) {
            WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
            GanttBarVO bar = new GanttBarVO();
            bar.setTaskId(task.getId());
            bar.setTaskCode(task.getCode());
            bar.setLoomId(task.getLoomId());
            bar.setWeavingOrderId(task.getWeavingOrderId());
            bar.setWeavingOrderCode(order != null ? order.getCode() : null);
            bar.setProductCode(order != null ? order.getProductCode() : null);
            bar.setProductName(order != null ? order.getProductName() : null);
            bar.setPlannedQty(task.getPlannedQty());
            bar.setActualQty(task.getActualQty());
            bar.setWorkerName(task.getWorkerName());
            bar.setPlannedStart(task.getPlannedStart());
            bar.setPlannedEnd(task.getPlannedEnd());
            bar.setStatus(task.getStatus());
            bar.setConflict(conflictTaskIds.contains(task.getId()));
            barVOs.add(bar);
        }

        return new GanttRespVO(loomVOs, barVOs);
    }

    @Override
    public PendingPoolRespVO getPendingPool() {
        // 组一：无日期待排任务（status=PENDING 且起止日期皆空）
        List<MachineTaskDO> unDatedTaskDOs = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getStatus, "PENDING")
                .isNull(MachineTaskDO::getPlannedStart)
                .isNull(MachineTaskDO::getPlannedEnd)
                .orderByDesc(MachineTaskDO::getId));

        List<PendingTaskVO> unDatedTasks = Collections.emptyList();
        if (CollUtil.isNotEmpty(unDatedTaskDOs)) {
            Set<Long> orderIds = unDatedTaskDOs.stream()
                    .map(MachineTaskDO::getWeavingOrderId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, WeavingOrderDO> orderMap = Collections.emptyMap();
            if (CollUtil.isNotEmpty(orderIds)) {
                List<WeavingOrderDO> orders = weavingOrderMapper.selectBatchIds(orderIds);
                if (CollUtil.isNotEmpty(orders)) {
                    orderMap = orders.stream().collect(Collectors.toMap(WeavingOrderDO::getId, Function.identity()));
                }
            }

            unDatedTasks = new ArrayList<>(unDatedTaskDOs.size());
            for (MachineTaskDO task : unDatedTaskDOs) {
                WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
                PendingTaskVO vo = new PendingTaskVO();
                vo.setTaskId(task.getId());
                vo.setTaskCode(task.getCode());
                vo.setWeavingOrderId(task.getWeavingOrderId());
                vo.setWeavingOrderCode(order != null ? order.getCode() : null);
                vo.setLoomId(task.getLoomId());
                vo.setLoomNo(task.getLoomNo());
                vo.setPlannedQty(task.getPlannedQty());
                unDatedTasks.add(vo);
            }
        }

        // 组二：未排满的织造单（DRAFT/ACTIVE，排除 CANCELLED/COMPLETED）
        List<WeavingOrderDO> activeOrders = weavingOrderMapper.selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .in(WeavingOrderDO::getStatus, List.of("DRAFT", "ACTIVE"))
                .orderByDesc(WeavingOrderDO::getId));

        List<PendingOrderVO> unScheduledOrders = Collections.emptyList();
        if (CollUtil.isNotEmpty(activeOrders)) {
            List<Long> orderIds = activeOrders.stream().map(WeavingOrderDO::getId).collect(Collectors.toList());
            List<MachineTaskDO> occupiedTasks = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                    .in(MachineTaskDO::getWeavingOrderId, orderIds)
                    .in(MachineTaskDO::getStatus, List.of("PENDING", "IN_PROGRESS", "HALTED")));

            Map<Long, Integer> scheduledQtyMap = occupiedTasks.stream()
                    .collect(Collectors.groupingBy(MachineTaskDO::getWeavingOrderId,
                            Collectors.summingInt(t -> t.getPlannedQty() != null ? t.getPlannedQty() : 0)));

            unScheduledOrders = new ArrayList<>();
            for (WeavingOrderDO order : activeOrders) {
                int plannedQty = order.getPlannedQty() != null ? order.getPlannedQty() : 0;
                int scheduledQty = scheduledQtyMap.getOrDefault(order.getId(), 0);
                int remainingQty = plannedQty - scheduledQty;
                if (remainingQty > 0) {
                    PendingOrderVO vo = new PendingOrderVO();
                    vo.setWeavingOrderId(order.getId());
                    vo.setWeavingOrderCode(order.getCode());
                    vo.setProductCode(order.getProductCode());
                    vo.setProductName(order.getProductName());
                    vo.setPlannedQty(plannedQty);
                    vo.setScheduledQty(scheduledQty);
                    vo.setRemainingQty(remainingQty);
                    unScheduledOrders.add(vo);
                }
            }
        }

        return new PendingPoolRespVO(unDatedTasks, unScheduledOrders);
    }

    private void validateGanttQuery(GanttQueryReqVO reqVO) {
        if (reqVO == null || reqVO.getStart() == null || reqVO.getEnd() == null) {
            throw invalidParamException("甘特窗口起止日期不能为空");
        }
        if (reqVO.getEnd().isBefore(reqVO.getStart())) {
            throw exception(MACHINE_TASK_DATE_RANGE_INVALID);
        }
        if (ChronoUnit.DAYS.between(reqVO.getStart(), reqVO.getEnd()) > 92) {
            throw invalidParamException("查询窗口跨度不能超过92天");
        }
    }

}

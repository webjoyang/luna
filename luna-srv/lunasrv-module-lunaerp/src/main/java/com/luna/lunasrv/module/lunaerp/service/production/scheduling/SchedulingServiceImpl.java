package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 生产管理 - 排产中心只读查询与聚合 Service 实现
 *
 * @author luna
 */
@Service
public class SchedulingServiceImpl implements SchedulingService {

    private static final Set<String> OCCUPIED_STATUSES = Set.of("PENDING", "IN_PROGRESS", "HALTED");

    @Resource
    private LoomMapper loomMapper;

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Override
    public GanttRespVO getGantt(GanttReqVO reqVO) {
        // 1. 窗口合法性与跨度校验（≤ 92 天）
        if (reqVO.getStart() == null || reqVO.getEnd() == null) {
            throw exception(GlobalErrorCodeConstants.BAD_REQUEST, "甘特窗口起止日期不能为空");
        }
        if (reqVO.getEnd().isBefore(reqVO.getStart())) {
            throw exception(GlobalErrorCodeConstants.BAD_REQUEST, "甘特窗口结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(reqVO.getStart(), reqVO.getEnd()) > 92) {
            throw exception(GlobalErrorCodeConstants.BAD_REQUEST, "甘特窗口跨度不能超过92天");
        }

        // 2. 查询 ACTIVE 织机行（含空行，INACTIVE 排除）
        List<LoomDO> loomDOList = loomMapper.selectList(new LambdaQueryWrapperX<LoomDO>()
                .eq(LoomDO::getStatus, "ACTIVE")
                .inIfPresent(LoomDO::getId, reqVO.getLoomIds())
                .orderByAsc(LoomDO::getLoomNo)
                .orderByAsc(LoomDO::getId));

        List<GanttLoomVO> looms = loomDOList.stream()
                .map(l -> new GanttLoomVO(l.getId(), l.getLoomNo(), l.getModel()))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(looms)) {
            return new GanttRespVO(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> activeLoomIds = looms.stream().map(GanttLoomVO::getLoomId).toList();

        // 3. 查询有效机台任务
        List<MachineTaskDO> taskList = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .in(MachineTaskDO::getLoomId, activeLoomIds)
                .inIfPresent(MachineTaskDO::getStatus, reqVO.getStatuses())
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByAsc(MachineTaskDO::getId));

        // 4. 过滤与窗口相交的条带（无日期任务不进入甘特）
        List<MachineTaskDO> intersectedTasks = new ArrayList<>();
        for (MachineTaskDO task : taskList) {
            LocalDate[] eff = coalesceDateRange(task.getPlannedStart(), task.getPlannedEnd());
            if (eff == null) {
                continue; // 无日期任务进待排池，不画甘特
            }
            // 闭区间相交：effStart <= windowEnd && effEnd >= windowStart
            if (!eff[0].isAfter(reqVO.getEnd()) && !eff[1].isBefore(reqVO.getStart())) {
                intersectedTasks.add(task);
            }
        }

        if (CollUtil.isEmpty(intersectedTasks)) {
            return new GanttRespVO(looms, Collections.emptyList());
        }

        // 5. 冲突判定数据源：同织机全部占用态任务
        List<MachineTaskDO> allOccupied = machineTaskMapper.selectOccupiedTasks(activeLoomIds);
        Set<Long> conflictTaskIds = findConflictTaskIds(allOccupied);

        // 6. 批量加载织造单快照（避免 N+1）
        Set<Long> orderIds = intersectedTasks.stream()
                .map(MachineTaskDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = batchLoadWeavingOrders(orderIds);

        // 7. 组装条带
        List<GanttBarVO> bars = new ArrayList<>(intersectedTasks.size());
        for (MachineTaskDO task : intersectedTasks) {
            GanttBarVO bar = new GanttBarVO();
            bar.setTaskId(task.getId());
            bar.setTaskCode(task.getCode());
            bar.setLoomId(task.getLoomId());
            bar.setWeavingOrderId(task.getWeavingOrderId());
            bar.setPlannedQty(task.getPlannedQty());
            bar.setActualQty(task.getActualQty());
            bar.setWorkerUserId(task.getWorkerUserId());
            bar.setWorkerName(task.getWorkerName());
            bar.setPlannedStart(task.getPlannedStart());
            bar.setPlannedEnd(task.getPlannedEnd());
            bar.setStatus(task.getStatus());

            WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
            if (order != null) {
                bar.setWeavingOrderCode(order.getCode());
                bar.setProductCode(order.getProductCode());
                bar.setProductName(order.getProductName());
            }

            bar.setConflict(conflictTaskIds.contains(task.getId()));
            bars.add(bar);
        }

        return new GanttRespVO(looms, bars);
    }

    @Override
    public PendingPoolRespVO getPendingPool() {
        // 1. 组一：无日期 PENDING 任务（status=PENDING 且 plannedStart IS NULL 且 plannedEnd IS NULL）
        List<MachineTaskDO> unDatedTaskDOs = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getStatus, "PENDING")
                .isNull(MachineTaskDO::getPlannedStart)
                .isNull(MachineTaskDO::getPlannedEnd)
                .orderByAsc(MachineTaskDO::getId));

        List<PendingTaskVO> unDatedTasks = Collections.emptyList();
        if (CollUtil.isNotEmpty(unDatedTaskDOs)) {
            Set<Long> orderIds = unDatedTaskDOs.stream()
                    .map(MachineTaskDO::getWeavingOrderId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, WeavingOrderDO> orderMap = batchLoadWeavingOrders(orderIds);

            unDatedTasks = new ArrayList<>(unDatedTaskDOs.size());
            for (MachineTaskDO task : unDatedTaskDOs) {
                PendingTaskVO vo = new PendingTaskVO();
                vo.setTaskId(task.getId());
                vo.setTaskCode(task.getCode());
                vo.setWeavingOrderId(task.getWeavingOrderId());
                vo.setLoomId(task.getLoomId());
                vo.setLoomNo(task.getLoomNo());
                vo.setPlannedQty(task.getPlannedQty());

                WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
                if (order != null) {
                    vo.setWeavingOrderCode(order.getCode());
                }
                unDatedTasks.add(vo);
            }
        }

        // 2. 组二：未排满的织造单（DRAFT/ACTIVE，排除 COMPLETED/CANCELLED）
        List<WeavingOrderDO> candidateOrders = weavingOrderMapper.selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .in(WeavingOrderDO::getStatus, List.of("DRAFT", "ACTIVE"))
                .orderByDesc(WeavingOrderDO::getId));

        List<PendingOrderVO> unScheduledOrders = Collections.emptyList();
        if (CollUtil.isNotEmpty(candidateOrders)) {
            List<Long> orderIds = candidateOrders.stream().map(WeavingOrderDO::getId).toList();

            // 批量查询占用态（PENDING/IN_PROGRESS/HALTED）任务计算 scheduledQty（无 N+1）
            List<MachineTaskDO> occupiedTasks = machineTaskMapper.selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                    .in(MachineTaskDO::getWeavingOrderId, orderIds)
                    .in(MachineTaskDO::getStatus, OCCUPIED_STATUSES));

            Map<Long, Integer> scheduledMap = new HashMap<>();
            if (CollUtil.isNotEmpty(occupiedTasks)) {
                for (MachineTaskDO task : occupiedTasks) {
                    if (task.getWeavingOrderId() != null && task.getPlannedQty() != null) {
                        scheduledMap.merge(task.getWeavingOrderId(), task.getPlannedQty(), Integer::sum);
                    }
                }
            }

            unScheduledOrders = new ArrayList<>();
            for (WeavingOrderDO order : candidateOrders) {
                int planned = order.getPlannedQty() != null ? order.getPlannedQty() : 0;
                int scheduled = scheduledMap.getOrDefault(order.getId(), 0);
                int remaining = planned - scheduled;
                if (remaining > 0) {
                    PendingOrderVO vo = new PendingOrderVO();
                    vo.setWeavingOrderId(order.getId());
                    vo.setWeavingOrderCode(order.getCode());
                    vo.setProductCode(order.getProductCode());
                    vo.setProductName(order.getProductName());
                    vo.setPlannedQty(order.getPlannedQty());
                    vo.setScheduledQty(scheduled);
                    vo.setRemainingQty(remaining);
                    unScheduledOrders.add(vo);
                }
            }
        }

        return new PendingPoolRespVO(unDatedTasks, unScheduledOrders);
    }

    @Override
    public PageResult<MachineTaskRespVO> getMachineTaskPage(MachineTaskPageReqVO reqVO) {
        boolean conflictOnly = Boolean.TRUE.equals(reqVO.getConflictOnly());

        Set<Long> conflictTaskIds = null;
        if (conflictOnly) {
            // 只看冲突红标任务：先计算所有冲突任务 ID 集合
            List<Long> filterLoomIds = CollUtil.isNotEmpty(reqVO.getLoomIds()) ? reqVO.getLoomIds()
                    : (reqVO.getLoomId() != null ? List.of(reqVO.getLoomId()) : null);
            List<MachineTaskDO> allOccupied = machineTaskMapper.selectOccupiedTasks(filterLoomIds);
            conflictTaskIds = findConflictTaskIds(allOccupied);
            if (CollUtil.isEmpty(conflictTaskIds)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
        }

        // 分页查询机台任务（支持限制 conflictTaskIds，并按 plannedStart 升序、createTime 降序排序）
        PageResult<MachineTaskDO> pageResult = machineTaskMapper.selectPage(reqVO, conflictTaskIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询织造单快照（无 N+1）
        Set<Long> orderIds = pageResult.getList().stream()
                .map(MachineTaskDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = batchLoadWeavingOrders(orderIds);

        // 若非 conflictOnly 模式，需计算当页各任务的 conflict 标记
        if (conflictTaskIds == null) {
            Set<Long> loomIdsOnPage = pageResult.getList().stream()
                    .map(MachineTaskDO::getLoomId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (CollUtil.isNotEmpty(loomIdsOnPage)) {
                List<MachineTaskDO> occupiedOnPage = machineTaskMapper.selectOccupiedTasks(loomIdsOnPage);
                conflictTaskIds = findConflictTaskIds(occupiedOnPage);
            } else {
                conflictTaskIds = Collections.emptySet();
            }
        }

        List<MachineTaskRespVO> respList = new ArrayList<>(pageResult.getList().size());
        for (MachineTaskDO task : pageResult.getList()) {
            MachineTaskRespVO respVO = BeanUtils.toBean(task, MachineTaskRespVO.class);

            WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
            if (order != null) {
                respVO.setWeavingOrderCode(order.getCode());
                respVO.setProductCode(order.getProductCode());
                respVO.setProductName(order.getProductName());
            }

            respVO.setConflict(conflictTaskIds.contains(task.getId()));
            respList.add(respVO);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    /**
     * 计算指定任务列表中所有时段冲突任务的 ID 集合（02 同口径）
     *
     * 规则：
     * 1. 仅占用态（PENDING, IN_PROGRESS, HALTED）任务参与冲突计算，COMPLETED 排除；
     * 2. 两端日期皆空者不占时段，跳过；
     * 3. 同织机闭区间重叠（eff1[0] <= eff2[1] && eff1[1] >= eff2[0]），重叠双方均计入冲突。
     */
    private Set<Long> findConflictTaskIds(List<MachineTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptySet();
        }

        Map<Long, List<MachineTaskDO>> byLoom = tasks.stream()
                .filter(t -> OCCUPIED_STATUSES.contains(t.getStatus()))
                .filter(t -> t.getLoomId() != null)
                .collect(Collectors.groupingBy(MachineTaskDO::getLoomId));

        Set<Long> conflictIds = new HashSet<>();
        for (List<MachineTaskDO> loomTasks : byLoom.values()) {
            int size = loomTasks.size();
            for (int i = 0; i < size; i++) {
                MachineTaskDO t1 = loomTasks.get(i);
                LocalDate[] eff1 = coalesceDateRange(t1.getPlannedStart(), t1.getPlannedEnd());
                if (eff1 == null) {
                    continue;
                }
                for (int j = i + 1; j < size; j++) {
                    MachineTaskDO t2 = loomTasks.get(j);
                    LocalDate[] eff2 = coalesceDateRange(t2.getPlannedStart(), t2.getPlannedEnd());
                    if (eff2 == null) {
                        continue;
                    }
                    // 闭区间相交判定
                    if (!eff1[0].isAfter(eff2[1]) && !eff1[1].isBefore(eff2[0])) {
                        conflictIds.add(t1.getId());
                        conflictIds.add(t2.getId());
                    }
                }
            }
        }
        return conflictIds;
    }

    private LocalDate[] coalesceDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return null;
        }
        LocalDate effStart = start != null ? start : end;
        LocalDate effEnd = end != null ? end : start;
        return new LocalDate[]{effStart, effEnd};
    }

    private Map<Long, WeavingOrderDO> batchLoadWeavingOrders(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<WeavingOrderDO> orders = weavingOrderMapper.selectBatchIds(orderIds);
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        return orders.stream().collect(Collectors.toMap(WeavingOrderDO::getId, o -> o));
    }

}

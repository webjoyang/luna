package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * {@link SchedulingServiceImpl} 单元测试
 *
 * 覆盖 Issue 04 核心规格：
 * 接缝 1：甘特图数据（getGantt）
 * 接缝 2：待排池数据（getPendingPool）
 * 接缝 3：跨单分页查询与冲突过滤（getMachineTaskPage）
 */
class SchedulingServiceImplTest {

    @Mock
    private LoomMapper loomMapper;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LoomDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MachineTaskDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeavingOrderDO.class);
    }

    // ==================== 接缝 1：甘特数据（getGantt） ====================

    @Test
    @DisplayName("甘特：窗口起止非法或跨度超过92天 -> 抛 BAD_REQUEST 异常")
    void testGantt_windowValidation_invalidRange_throws() {
        // 跨度 93 天
        GanttReqVO reqOver92 = new GanttReqVO();
        reqOver92.setStart(LocalDate.of(2026, 1, 1));
        reqOver92.setEnd(LocalDate.of(2026, 4, 15)); // 104 天 > 92

        ServiceException ex1 = assertThrows(ServiceException.class, () -> schedulingService.getGantt(reqOver92));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex1.getCode());

        // start > end
        GanttReqVO reqInverted = new GanttReqVO();
        reqInverted.setStart(LocalDate.of(2026, 10, 10));
        reqInverted.setEnd(LocalDate.of(2026, 10, 1));

        ServiceException ex2 = assertThrows(ServiceException.class, () -> schedulingService.getGantt(reqInverted));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("甘特：返回全部 ACTIVE 织机（含无任务空行），INACTIVE 排除")
    void testGantt_returnsAllActiveLooms_includingEmptyRow_excludesInactive() {
        LoomDO loom1 = buildLoom(1L, "LM-01", "喷气 230", "ACTIVE");
        LoomDO loom2 = buildLoom(2L, "LM-02", "剑杆 190", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1, loom2));
        when(machineTaskMapper.selectList(any())).thenReturn(Collections.emptyList());

        GanttReqVO req = new GanttReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 15));

        GanttRespVO resp = schedulingService.getGantt(req);

        assertNotNull(resp);
        assertEquals(2, resp.getLooms().size());
        assertEquals(1L, resp.getLooms().get(0).getLoomId());
        assertEquals("LM-01", resp.getLooms().get(0).getLoomNo());
        assertEquals(2L, resp.getLooms().get(1).getLoomId());
        assertTrue(resp.getBars().isEmpty(), "两台织机均无任务，条带应为空行");
    }

    @Test
    @DisplayName("甘特：条带日期与窗口相交才返回；窗口外及无日期任务排除")
    void testGantt_taskDateFiltering_withinWindowOnly_skipsUndated() {
        LoomDO loom1 = buildLoom(1L, "LM-01", "喷气 230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1));

        // 窗口：2026-10-05 ~ 2026-10-15
        // 任务 1：10-01 ~ 10-06（相交，应返回）
        MachineTaskDO task1 = buildTask(10L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));
        // 任务 2：10-16 ~ 10-20（窗口外，不返回）
        MachineTaskDO task2 = buildTask(20L, "MT-02", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 20));
        // 任务 3：无日期（待排池，甘特不返回）
        MachineTaskDO task3 = buildTask(30L, "MT-03", 1L, 100L, "PENDING", null, null);

        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task1, task2, task3));

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttReqVO req = new GanttReqVO();
        req.setStart(LocalDate.of(2026, 10, 5));
        req.setEnd(LocalDate.of(2026, 10, 15));

        GanttRespVO resp = schedulingService.getGantt(req);

        assertEquals(1, resp.getBars().size());
        assertEquals(10L, resp.getBars().get(0).getTaskId());
    }

    @Test
    @DisplayName("甘特：同织机两占用态任务时段重叠 -> 双方 conflict 均标 true")
    void testGantt_conflictDetection_twoOccupiedTasksOverlap_bothConflictTrue() {
        LoomDO loom1 = buildLoom(1L, "LM-01", "喷气 230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1));

        // 任务 1：10-08 ~ 10-12 PENDING
        MachineTaskDO task1 = buildTask(10L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        // 任务 2：10-10 ~ 10-15 IN_PROGRESS（闭区间相交：10-10 ~ 10-12 重叠）
        MachineTaskDO task2 = buildTask(20L, "MT-02", 1L, 100L, "IN_PROGRESS",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        // occupiedTasks 包含 task1, task2
        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1, task2));

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttReqVO req = new GanttReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGantt(req);

        assertEquals(2, resp.getBars().size());
        GanttBarVO bar1 = resp.getBars().stream().filter(b -> b.getTaskId().equals(10L)).findFirst().orElseThrow();
        GanttBarVO bar2 = resp.getBars().stream().filter(b -> b.getTaskId().equals(20L)).findFirst().orElseThrow();

        assertTrue(bar1.getConflict(), "任务1与任务2重叠，应标红 conflict=true");
        assertTrue(bar2.getConflict(), "任务2与任务1重叠，应标红 conflict=true");
    }

    @Test
    @DisplayName("甘特：COMPLETED 任务自身 conflict=false，且不令重叠的他任务标冲突")
    void testGantt_conflictDetection_completedTask_noConflict_doesNotCauseOthersConflict() {
        LoomDO loom1 = buildLoom(1L, "LM-01", "喷气 230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1));

        // 任务 1：10-08 ~ 10-12 PENDING
        MachineTaskDO task1 = buildTask(10L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        // 任务 2：10-10 ~ 10-15 COMPLETED
        MachineTaskDO task2 = buildTask(20L, "MT-02", 1L, 100L, "COMPLETED",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        // occupiedTasks 仅含 task1，COMPLETED 不属于占用态
        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1));

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttReqVO req = new GanttReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGantt(req);

        assertEquals(2, resp.getBars().size());
        GanttBarVO bar1 = resp.getBars().stream().filter(b -> b.getTaskId().equals(10L)).findFirst().orElseThrow();
        GanttBarVO bar2 = resp.getBars().stream().filter(b -> b.getTaskId().equals(20L)).findFirst().orElseThrow();

        assertFalse(bar1.getConflict(), "COMPLETED 不触发他人冲突，任务1应为 false");
        assertFalse(bar2.getConflict(), "COMPLETED 任务自身恒为 false");
    }

    @Test
    @DisplayName("甘特：织造单号/产品快照批量查询无 N+1")
    void testGantt_snapshotsLoadedWithoutNPlusOne() {
        LoomDO loom1 = buildLoom(1L, "LM-01", "喷气 230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1));

        MachineTaskDO task1 = buildTask(10L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskDO task2 = buildTask(20L, "MT-02", 1L, 101L, "PENDING",
                LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1, task2));

        WeavingOrderDO order1 = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        WeavingOrderDO order2 = buildWeavingOrder(101L, "ZZ-02", "P02", "涤纶");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order1, order2));

        GanttReqVO req = new GanttReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGantt(req);

        assertEquals(2, resp.getBars().size());
        assertEquals("ZZ-01", resp.getBars().get(0).getWeavingOrderCode());
        assertEquals("纯棉", resp.getBars().get(0).getProductName());
        assertEquals("ZZ-02", resp.getBars().get(1).getWeavingOrderCode());
        assertEquals("涤纶", resp.getBars().get(1).getProductName());

        // 验证批量查询被调用且仅调用 1 次，无 N+1
        verify(weavingOrderMapper, times(1)).selectBatchIds(any());
        verify(weavingOrderMapper, never()).selectById(any());
    }

    // ==================== 接缝 2：待排池数据（getPendingPool） ====================

    @Test
    @DisplayName("待排池组一：无日期 PENDING 任务入组一；有日期任务或非 PENDING 排除")
    void testPendingPool_groupOne_unDatedPendingTasks_included_datedExcluded() {
        // 组一候选任务
        MachineTaskDO task1 = buildTask(11L, "MT-01", 1L, 100L, "PENDING", null, null); // 满足
        task1.setLoomNo("LM-01");
        MachineTaskDO task2 = buildTask(12L, "MT-02", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)); // 有日期，排除
        MachineTaskDO task3 = buildTask(13L, "MT-03", 1L, 100L, "IN_PROGRESS", null, null); // 非 PENDING，排除

        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task1)); // machineTaskMapper 查询 status=PENDING, plannedStart IS NULL, plannedEnd IS NULL
        when(weavingOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        PendingPoolRespVO resp = schedulingService.getPendingPool();

        assertNotNull(resp);
        assertEquals(1, resp.getUnDatedTasks().size());
        assertEquals(11L, resp.getUnDatedTasks().get(0).getTaskId());
        assertEquals("ZZ-01", resp.getUnDatedTasks().get(0).getWeavingOrderCode());
        assertEquals("LM-01", resp.getUnDatedTasks().get(0).getLoomNo());
    }

    @Test
    @DisplayName("待排池组二：未排满 DRAFT/ACTIVE 织造单入池，scheduledQty 为占用态匹数合计，remainingQty 正确")
    void testPendingPool_groupTwo_unScheduledOrders_scheduledQtyAndRemainingQtyCorrect() {
        WeavingOrderDO order1 = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        order1.setStatus("DRAFT");
        order1.setPlannedQty(50);

        WeavingOrderDO order2 = buildWeavingOrder(200L, "ZZ-02", "P02", "涤纶");
        order2.setStatus("ACTIVE");
        order2.setPlannedQty(60);

        when(weavingOrderMapper.selectList(any())).thenReturn(List.of(order1, order2));

        // order1 下有两个占用态任务：20匹 PENDING，10匹 IN_PROGRESS -> scheduledQty = 30, remainingQty = 20
        MachineTaskDO t1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING", null, null);
        t1.setPlannedQty(20);
        MachineTaskDO t2 = buildTask(2L, "MT-02", 1L, 100L, "IN_PROGRESS", null, null);
        t2.setPlannedQty(10);
        // 以及 1 个 COMPLETED 任务 15 匹 -> 不计入 scheduledQty
        MachineTaskDO t3 = buildTask(3L, "MT-03", 1L, 100L, "COMPLETED", null, null);
        t3.setPlannedQty(15);

        // order2 下无任务 -> scheduledQty = 0, remainingQty = 60
        // mock machineTaskMapper.selectOccupiedTasksByWeavingOrderIds(ids) or selectList
        when(machineTaskMapper.selectList(any())).thenReturn(Collections.emptyList(), List.of(t1, t2));

        PendingPoolRespVO resp = schedulingService.getPendingPool();

        assertNotNull(resp);
        assertEquals(2, resp.getUnScheduledOrders().size());

        PendingOrderVO o1 = resp.getUnScheduledOrders().stream().filter(o -> o.getWeavingOrderId().equals(100L)).findFirst().orElseThrow();
        assertEquals(50, o1.getPlannedQty());
        assertEquals(30, o1.getScheduledQty());
        assertEquals(20, o1.getRemainingQty());

        PendingOrderVO o2 = resp.getUnScheduledOrders().stream().filter(o -> o.getWeavingOrderId().equals(200L)).findFirst().orElseThrow();
        assertEquals(60, o2.getPlannedQty());
        assertEquals(0, o2.getScheduledQty());
        assertEquals(60, o2.getRemainingQty());
    }

    @Test
    @DisplayName("待排池组二：已排满订单（remainingQty <= 0）及 COMPLETED/CANCELLED 单排除")
    void testPendingPool_groupTwo_fullyScheduledAndFinishedOrders_excluded() {
        // order1 计划 50 匹，已排 50 匹 -> remainingQty = 0 -> 排除
        WeavingOrderDO order1 = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉");
        order1.setStatus("ACTIVE");
        order1.setPlannedQty(50);

        MachineTaskDO t1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING", null, null);
        t1.setPlannedQty(50);

        // 仅 order1 返回（COMPLETED/CANCELLED 已在 SQL 层通过 eq/in 过滤）
        when(weavingOrderMapper.selectList(any())).thenReturn(List.of(order1));
        when(machineTaskMapper.selectList(any())).thenReturn(Collections.emptyList(), List.of(t1));

        PendingPoolRespVO resp = schedulingService.getPendingPool();

        assertNotNull(resp);
        assertTrue(resp.getUnScheduledOrders().isEmpty(), "已排满订单不入待排池");
    }

    // ==================== 接缝 3：跨单分页查询与冲突过滤（getMachineTaskPage） ====================

    @Test
    @DisplayName("跨单分页：weavingOrderId 为空时返回跨单数据，批量查询快照无 N+1")
    void testMachineTaskPage_crossOrderQuery_weavingOrderIdNull_loadsSnapshotsWithoutNPlusOne() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setWeavingOrderId(null); // 缺省跨单

        MachineTaskDO task1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskDO task2 = buildTask(2L, "MT-02", 2L, 200L, "IN_PROGRESS",
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 15));
        PageResult<MachineTaskDO> pagedDO = new PageResult<>(List.of(task1, task2), 2L);

        when(machineTaskMapper.selectPage(eq(req), nullable(Collection.class))).thenReturn(pagedDO);
        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1, task2));

        WeavingOrderDO order1 = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉汗布");
        WeavingOrderDO order2 = buildWeavingOrder(200L, "ZZ-02", "P02", "双面卫衣布");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order1, order2));

        PageResult<MachineTaskRespVO> pageResult = schedulingService.getMachineTaskPage(req);

        assertEquals(2L, pageResult.getTotal());
        assertEquals("ZZ-01", pageResult.getList().get(0).getWeavingOrderCode());
        assertEquals("纯棉汗布", pageResult.getList().get(0).getProductName());
        assertEquals("ZZ-02", pageResult.getList().get(1).getWeavingOrderCode());
        assertEquals("双面卫衣布", pageResult.getList().get(1).getProductName());

        // 验证批量查询无 N+1
        verify(weavingOrderMapper, times(1)).selectBatchIds(any());
        verify(weavingOrderMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("跨单分页：conflictOnly=true 仅过滤时段冲突红标任务")
    void testMachineTaskPage_conflictOnly_filtersOnlyConflictedTasks() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setConflictOnly(true);

        // 同织机 1 上：task1(10-08~10-12) 与 task2(10-10~10-14) 冲突；task3(10-20~10-25) 不冲突
        MachineTaskDO task1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskDO task2 = buildTask(2L, "MT-02", 1L, 100L, "IN_PROGRESS",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14));
        MachineTaskDO task3 = buildTask(3L, "MT-03", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25));

        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1, task2, task3));

        // 预期将冲突任务集合 [1L, 2L] 传入 selectPage 进行分页过滤
        PageResult<MachineTaskDO> conflictPage = new PageResult<>(List.of(task1, task2), 2L);
        when(machineTaskMapper.selectPage(eq(req), nullable(Collection.class))).thenReturn(conflictPage);

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉汗布");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        PageResult<MachineTaskRespVO> pageResult = schedulingService.getMachineTaskPage(req);

        assertEquals(2L, pageResult.getTotal());
        assertTrue(pageResult.getList().stream().allMatch(MachineTaskRespVO::getConflict), "返回的任务 conflict 均为 true");
    }

    @Test
    @DisplayName("跨单分页：COMPLETED 任务自身不冲突且不使他人冲突")
    void testMachineTaskPage_conflictFlagCalculation_completedTask_false() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();

        // task1 为 PENDING，task2 为 COMPLETED，二者同织机且时段相交
        MachineTaskDO task1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskDO task2 = buildTask(2L, "MT-02", 1L, 100L, "COMPLETED",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14));

        PageResult<MachineTaskDO> pageDO = new PageResult<>(List.of(task1, task2), 2L);
        when(machineTaskMapper.selectPage(eq(req), nullable(Collection.class))).thenReturn(pageDO);

        // occupiedTasks 仅返回 task1，COMPLETED 不在占用态列表中
        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(List.of(task1));

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉汗布");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        PageResult<MachineTaskRespVO> pageResult = schedulingService.getMachineTaskPage(req);

        assertEquals(2L, pageResult.getTotal());
        assertFalse(pageResult.getList().get(0).getConflict(), "task1 与 COMPLETED 重叠不标冲突");
        assertFalse(pageResult.getList().get(1).getConflict(), "COMPLETED 任务自身不标冲突");
    }

    @Test
    @DisplayName("跨单分页：conflictOnly=true 且库内无任何冲突任务时直接返回空分页")
    void testMachineTaskPage_conflictOnly_noConflicts_returnsEmptyPageImmediately() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setConflictOnly(true);

        when(machineTaskMapper.selectOccupiedTasks(any())).thenReturn(Collections.emptyList());

        PageResult<MachineTaskRespVO> pageResult = schedulingService.getMachineTaskPage(req);

        assertNotNull(pageResult);
        assertEquals(0L, pageResult.getTotal());
        assertTrue(pageResult.getList().isEmpty());
        verify(machineTaskMapper, never()).selectPage(any(), nullable(Collection.class));
    }

    @Test
    @DisplayName("跨单分页：支持 loomIds 列表过滤与冲突计算")
    void testMachineTaskPage_loomIdsList_filtersSpecifiedLooms() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setLoomIds(List.of(1L, 2L));
        req.setConflictOnly(true);

        MachineTaskDO task1 = buildTask(1L, "MT-01", 1L, 100L, "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskDO task2 = buildTask(2L, "MT-02", 1L, 100L, "IN_PROGRESS",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14));

        when(machineTaskMapper.selectOccupiedTasks(eq(List.of(1L, 2L)))).thenReturn(List.of(task1, task2));
        PageResult<MachineTaskDO> conflictPage = new PageResult<>(List.of(task1, task2), 2L);
        when(machineTaskMapper.selectPage(eq(req), nullable(Collection.class))).thenReturn(conflictPage);

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "P01", "纯棉汗布");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        PageResult<MachineTaskRespVO> pageResult = schedulingService.getMachineTaskPage(req);

        assertEquals(2L, pageResult.getTotal());
        assertTrue(pageResult.getList().stream().allMatch(MachineTaskRespVO::getConflict));
        verify(machineTaskMapper, times(1)).selectOccupiedTasks(eq(List.of(1L, 2L)));
    }

    // ==================== 辅助方法 ====================

    private LoomDO buildLoom(Long id, String loomNo, String model, String status) {
        LoomDO loom = new LoomDO();
        loom.setId(id);
        loom.setLoomNo(loomNo);
        loom.setModel(model);
        loom.setStatus(status);
        return loom;
    }

    private MachineTaskDO buildTask(Long id, String code, Long loomId, Long orderId, String status,
                                    LocalDate start, LocalDate end) {
        MachineTaskDO task = new MachineTaskDO();
        task.setId(id);
        task.setCode(code);
        task.setLoomId(loomId);
        task.setWeavingOrderId(orderId);
        task.setStatus(status);
        task.setPlannedQty(30);
        task.setActualQty(10);
        task.setWorkerName("张三");
        task.setPlannedStart(start);
        task.setPlannedEnd(end);
        return task;
    }

    private WeavingOrderDO buildWeavingOrder(Long id, String code, String productCode, String productName) {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(id);
        order.setCode(code);
        order.setProductCode(productCode);
        order.setProductName(productName);
        order.setPlannedQty(100);
        return order;
    }

}

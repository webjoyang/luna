package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.service.production.loom.LoomService;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task.MachineTaskServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link SchedulingServiceImpl} 与排产中心跨单分页/冲突只读单元测试
 *
 * 覆盖 Spec §3 及 Testing Decisions 7-10 全部场景：
 * 7. 甘特返回全部 ACTIVE 织机（含窗口内无任务空行），INACTIVE 织机不返回；窗口外条带不返回；起止跨度校验。
 * 8. conflict 标记：存量互相重叠的两条任务均标 true；COMPLETED 参与展示但不令他任务标冲突。
 * 9. 待排池：无日期 PENDING 任务入组一；Σ占用态任务匹数 < plannedQty 的 DRAFT/ACTIVE 织造单入组二且 remainingQty 正确；COMPLETED/CANCELLED 单排除。
 * 10. 跨单分页：weavingOrderId 缺省返回多单数据；conflictOnly 过滤生效；快照字段无 N+1（批量查询验证）。
 */
class SchedulingServiceImplTest {

    @Mock
    private LoomMapper loomMapper;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private LoomService loomService;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    @InjectMocks
    private MachineTaskServiceImpl machineTaskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LoomDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MachineTaskDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeavingOrderDO.class);
    }

    // ==================== Testing Decision 7: 甘特织机行与窗口过滤 ====================

    @Test
    @DisplayName("D7-1: 甘特返回全部 ACTIVE 织机（含窗口内无任务空行），INACTIVE 织机不返回")
    void testGantt_returnsAllActiveLooms_includingEmptyRows_excludesInactive() {
        LoomDO loom1 = createLoom(1L, "LM-01", "喷气230", "ACTIVE");
        LoomDO loom2 = createLoom(2L, "LM-02", "剑杆190", "ACTIVE");
        // loomMapper.selectList 模拟仅返回 ACTIVE 织机
        when(loomMapper.selectList(any())).thenReturn(List.of(loom1, loom2));

        // 仅 loom1 在窗口内有任务，loom2 为空行
        MachineTaskDO task = createTask(10L, "MT-01", 1L, 100L,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10), "IN_PROGRESS", 20);
        when(machineTaskMapper.selectOccupiedTasksByLoomIds(any())).thenReturn(List.of(task));
        when(machineTaskMapper.selectList(any())).thenReturn(List.of(task));

        WeavingOrderDO order = createOrder(100L, "WO-01", "P01", "汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttQueryReqVO req = new GanttQueryReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGanttData(req);

        assertNotNull(resp);
        assertEquals(2, resp.getLooms().size(), "包含窗口内无任务的空行 loom2");
        assertEquals("LM-01", resp.getLooms().get(0).getLoomNo());
        assertEquals("LM-02", resp.getLooms().get(1).getLoomNo());

        assertEquals(1, resp.getBars().size());
        assertEquals("MT-01", resp.getBars().get(0).getTaskCode());
        assertEquals("WO-01", resp.getBars().get(0).getWeavingOrderCode());
    }

    @Test
    @DisplayName("D7-2: 窗口外条带不返回，相交条带返回")
    void testGantt_barsOutsideWindowExcluded_intersectingIncluded() {
        LoomDO loom = createLoom(1L, "LM-01", "喷气230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom));

        // 任务1：完全在窗口之前 (09-20 ~ 09-25)
        MachineTaskDO taskBefore = createTask(1L, "MT-BEFORE", 1L, 100L,
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 25), "PENDING", 10);
        // 任务2：完全在窗口之后 (11-05 ~ 11-10)
        MachineTaskDO taskAfter = createTask(2L, "MT-AFTER", 1L, 100L,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 10), "PENDING", 10);
        // 任务3：与窗口相交 (10-15 ~ 10-25)
        MachineTaskDO taskInside = createTask(3L, "MT-INSIDE", 1L, 100L,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 25), "PENDING", 10);
        // 任务4：仅计划开始在窗口末日 (10-31 ~ 11-02，相交)
        MachineTaskDO taskEdge = createTask(4L, "MT-EDGE", 1L, 100L,
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2), "PENDING", 10);

        when(machineTaskMapper.selectOccupiedTasksByLoomIds(any())).thenReturn(List.of(taskBefore, taskAfter, taskInside, taskEdge));
        when(machineTaskMapper.selectList(any())).thenReturn(List.of(taskBefore, taskAfter, taskInside, taskEdge));

        WeavingOrderDO order = createOrder(100L, "WO-01", "P01", "汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttQueryReqVO req = new GanttQueryReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGanttData(req);

        assertEquals(2, resp.getBars().size(), "仅相交的两个任务返回");
        List<String> codes = resp.getBars().stream().map(GanttBarVO::getTaskCode).toList();
        assertTrue(codes.contains("MT-INSIDE"));
        assertTrue(codes.contains("MT-EDGE"));
        assertFalse(codes.contains("MT-BEFORE"));
        assertFalse(codes.contains("MT-AFTER"));
    }

    @Test
    @DisplayName("D7-3: 甘特查询参数非法校验：跨度 > 92 天或 end < start 抛异常")
    void testGantt_invalidParameters_throwsException() {
        GanttQueryReqVO reqOver92 = new GanttQueryReqVO();
        reqOver92.setStart(LocalDate.of(2026, 1, 1));
        reqOver92.setEnd(LocalDate.of(2026, 5, 1)); // 超过92天
        assertThrows(ServiceException.class, () -> schedulingService.getGanttData(reqOver92));

        GanttQueryReqVO reqEndBeforeStart = new GanttQueryReqVO();
        reqEndBeforeStart.setStart(LocalDate.of(2026, 10, 20));
        reqEndBeforeStart.setEnd(LocalDate.of(2026, 10, 10)); // end < start
        assertThrows(ServiceException.class, () -> schedulingService.getGanttData(reqEndBeforeStart));
    }

    // ==================== Testing Decision 8: conflict 标记同源计算 ====================

    @Test
    @DisplayName("D8-1: 存量互相重叠的两条占用态任务均标 true")
    void testGantt_overlappingOccupiedTasks_bothMarkedConflict() {
        LoomDO loom = createLoom(1L, "LM-01", "喷气230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom));

        // 两个重叠任务在同织机：任务A (10-05 ~ 10-12)，任务B (10-10 ~ 10-15)
        MachineTaskDO taskA = createTask(1L, "MT-A", 1L, 100L,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), "PENDING", 10);
        MachineTaskDO taskB = createTask(2L, "MT-B", 1L, 100L,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15), "IN_PROGRESS", 10);

        when(machineTaskMapper.selectOccupiedTasksByLoomIds(any())).thenReturn(List.of(taskA, taskB));
        when(machineTaskMapper.selectList(any())).thenReturn(List.of(taskA, taskB));

        WeavingOrderDO order = createOrder(100L, "WO-01", "P01", "汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttQueryReqVO req = new GanttQueryReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGanttData(req);

        assertEquals(2, resp.getBars().size());
        assertTrue(resp.getBars().get(0).getConflict(), "互叠任务 A 标 conflict=true");
        assertTrue(resp.getBars().get(1).getConflict(), "互叠任务 B 标 conflict=true");
    }

    @Test
    @DisplayName("D8-2: COMPLETED 参与展示但 conflict=false，且不令重叠的他任务标冲突")
    void testGantt_completedTask_showsInBars_notConflict_andDoesNotConflictOthers() {
        LoomDO loom = createLoom(1L, "LM-01", "喷气230", "ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(loom));

        // 任务A：已完工 COMPLETED (10-05 ~ 10-12)
        MachineTaskDO taskCompleted = createTask(1L, "MT-COMPLETED", 1L, 100L,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), "COMPLETED", 10);
        // 任务B：待开始 PENDING (10-10 ~ 10-15)，与已完工任务A时段重叠
        MachineTaskDO taskPending = createTask(2L, "MT-PENDING", 1L, 100L,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15), "PENDING", 10);

        // selectOccupiedTasksByLoomIds 仅返回占用态任务（不含 COMPLETED）
        when(machineTaskMapper.selectOccupiedTasksByLoomIds(any())).thenReturn(List.of(taskPending));
        // selectList (候选条带) 包含 COMPLETED 和 PENDING
        when(machineTaskMapper.selectList(any())).thenReturn(List.of(taskCompleted, taskPending));

        WeavingOrderDO order = createOrder(100L, "WO-01", "P01", "汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        GanttQueryReqVO req = new GanttQueryReqVO();
        req.setStart(LocalDate.of(2026, 10, 1));
        req.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO resp = schedulingService.getGanttData(req);

        assertEquals(2, resp.getBars().size());
        GanttBarVO barComp = resp.getBars().stream()
                .filter(b -> "MT-COMPLETED".equals(b.getTaskCode())).findFirst().orElseThrow();
        GanttBarVO barPend = resp.getBars().stream()
                .filter(b -> "MT-PENDING".equals(b.getTaskCode())).findFirst().orElseThrow();

        assertFalse(barComp.getConflict(), "COMPLETED 任务自身不标冲突");
        assertFalse(barPend.getConflict(), "与 COMPLETED 重叠的任务不被标冲突");
    }

    // ==================== Testing Decision 9: 待排池两组口径 ====================

    @Test
    @DisplayName("D9-1: 待排池组一：无日期 PENDING 任务入池；有日期任务不入池")
    void testPendingPool_group1_unDatedPendingTasks() {
        // 无日期任务：plannedStart 与 plannedEnd 均为 null
        MachineTaskDO unDatedTask = createTask(1L, "MT-UNDATED", 1L, 100L,
                null, null, "PENDING", 15);
        unDatedTask.setLoomNo("LM-01");

        when(machineTaskMapper.selectList(any())).thenReturn(List.of(unDatedTask)).thenReturn(Collections.emptyList());

        WeavingOrderDO order = createOrder(100L, "WO-01", "P01", "汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(weavingOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        PendingPoolRespVO resp = schedulingService.getPendingPool();

        assertNotNull(resp);
        assertEquals(1, resp.getUnDatedTasks().size());
        PendingTaskVO taskVO = resp.getUnDatedTasks().get(0);
        assertEquals("MT-UNDATED", taskVO.getTaskCode());
        assertEquals("WO-01", taskVO.getWeavingOrderCode());
        assertEquals("LM-01", taskVO.getLoomNo());
        assertEquals(15, taskVO.getPlannedQty());
    }

    @Test
    @DisplayName("D9-2: 待排池组二：未排满 DRAFT/ACTIVE 织造单入池，完全无任务单入池，已排满/COMPLETED/CANCELLED 排除")
    void testPendingPool_group2_unScheduledOrders() {
        // 组一模拟为空
        when(machineTaskMapper.selectList(argThat(wrapper -> wrapper != null))).thenReturn(Collections.emptyList());

        // 织造单1: DRAFT, plannedQty=50, 已排 30 -> remainingQty=20 > 0 (入池)
        WeavingOrderDO order1 = createOrder(101L, "WO-101", "P01", "汗布", 50, "DRAFT");
        // 织造单2: ACTIVE, plannedQty=100, 完全无任务 -> scheduledQty=0, remainingQty=100 (入池)
        WeavingOrderDO order2 = createOrder(102L, "WO-102", "P02", "斜纹", 100, "ACTIVE");
        // 织造单3: ACTIVE, plannedQty=40, 已排 40 -> remainingQty=0 (已排满，不入池)
        WeavingOrderDO order3 = createOrder(103L, "WO-103", "P03", "罗纹", 40, "ACTIVE");

        when(weavingOrderMapper.selectList(any())).thenReturn(List.of(order1, order2, order3));

        // 任务列表
        MachineTaskDO taskOrder1 = createTask(1L, "MT-101-1", 1L, 101L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "IN_PROGRESS", 30);
        MachineTaskDO taskOrder3 = createTask(2L, "MT-103-1", 1L, 103L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "PENDING", 40);

        when(machineTaskMapper.selectList(any()))
                .thenReturn(Collections.emptyList()) // group 1
                .thenReturn(List.of(taskOrder1, taskOrder3)); // group 2 tasks

        PendingPoolRespVO resp = schedulingService.getPendingPool();

        assertEquals(2, resp.getUnScheduledOrders().size());

        PendingOrderVO poolOrder1 = resp.getUnScheduledOrders().stream()
                .filter(o -> o.getWeavingOrderId().equals(101L)).findFirst().orElseThrow();
        assertEquals(50, poolOrder1.getPlannedQty());
        assertEquals(30, poolOrder1.getScheduledQty());
        assertEquals(20, poolOrder1.getRemainingQty());

        PendingOrderVO poolOrder2 = resp.getUnScheduledOrders().stream()
                .filter(o -> o.getWeavingOrderId().equals(102L)).findFirst().orElseThrow();
        assertEquals(100, poolOrder2.getPlannedQty());
        assertEquals(0, poolOrder2.getScheduledQty());
        assertEquals(100, poolOrder2.getRemainingQty());

        assertTrue(resp.getUnScheduledOrders().stream().noneMatch(o -> o.getWeavingOrderId().equals(103L)),
                "已排满织造单 103 不入池");
    }

    // ==================== Testing Decision 10: 跨单分页与批量查询 ====================

    @Test
    @DisplayName("D10-1: 跨单分页 weavingOrderId 缺省返回多单数据，批量填充织造单快照无 N+1")
    void testMachineTaskPage_crossOrder_withoutNPlus1() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setWeavingOrderId(null); // 缺省跨单

        MachineTaskDO t1 = createTask(1L, "MT-01", 1L, 101L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "PENDING", 10);
        MachineTaskDO t2 = createTask(2L, "MT-02", 2L, 102L,
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6), "IN_PROGRESS", 20);

        PageResult<MachineTaskDO> pageDO = new PageResult<>(List.of(t1, t2), 2L);
        when(machineTaskMapper.selectPage(eq(req))).thenReturn(pageDO);

        // 模拟批量查询织造单快照
        WeavingOrderDO o1 = createOrder(101L, "WO-101", "P01", "纯棉汗布", 50, "ACTIVE");
        WeavingOrderDO o2 = createOrder(102L, "WO-102", "P02", "纯棉斜纹", 80, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(o1, o2));

        when(machineTaskMapper.selectOccupiedTasksByLoomIds(anyCollection())).thenReturn(Collections.emptyList());

        PageResult<MachineTaskRespVO> pageResp = machineTaskService.getMachineTaskPage(req);

        assertEquals(2L, pageResp.getTotal());
        assertEquals(2, pageResp.getList().size());

        MachineTaskRespVO r1 = pageResp.getList().get(0);
        assertEquals("WO-101", r1.getWeavingOrderCode());
        assertEquals("P01", r1.getProductCode());
        assertEquals("纯棉汗布", r1.getProductName());

        MachineTaskRespVO r2 = pageResp.getList().get(1);
        assertEquals("WO-102", r2.getWeavingOrderCode());
        assertEquals("P02", r2.getProductCode());
        assertEquals("纯棉斜纹", r2.getProductName());

        // 验证仅调用一次 selectBatchIds，无 N+1 查询
        verify(weavingOrderMapper, times(1)).selectBatchIds(anyCollection());
    }

    @Test
    @DisplayName("D10-2: conflictOnly=true 过滤生效，仅返回冲突任务")
    void testMachineTaskPage_conflictOnly_filterWorks() {
        MachineTaskPageReqVO req = new MachineTaskPageReqVO();
        req.setConflictOnly(true);

        // 占用态任务，其中任务 1 和 2 重叠冲突，任务 3 不冲突
        MachineTaskDO t1 = createTask(1L, "MT-01", 1L, 101L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "PENDING", 10);
        MachineTaskDO t2 = createTask(2L, "MT-02", 1L, 101L,
                LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 8), "IN_PROGRESS", 20);
        MachineTaskDO t3 = createTask(3L, "MT-03", 1L, 101L,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25), "PENDING", 30);

        when(machineTaskMapper.selectOccupiedTasksByLoomIds(any())).thenReturn(List.of(t1, t2, t3));

        // 模拟带 ID 过滤的分页调用仅返回 t1, t2
        PageResult<MachineTaskDO> conflictPage = new PageResult<>(List.of(t1, t2), 2L);
        when(machineTaskMapper.selectPageWithIds(eq(req), anyCollection())).thenReturn(conflictPage);

        WeavingOrderDO o1 = createOrder(101L, "WO-101", "P01", "纯棉汗布", 50, "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(o1));

        PageResult<MachineTaskRespVO> pageResp = machineTaskService.getMachineTaskPage(req);

        assertEquals(2L, pageResp.getTotal());
        assertEquals(2, pageResp.getList().size());
        assertTrue(pageResp.getList().get(0).getConflict());
        assertTrue(pageResp.getList().get(1).getConflict());
    }

    // ==================== 辅助方法 ====================

    private LoomDO createLoom(Long id, String loomNo, String model, String status) {
        LoomDO loom = new LoomDO();
        loom.setId(id);
        loom.setLoomNo(loomNo);
        loom.setModel(model);
        loom.setStatus(status);
        return loom;
    }

    private MachineTaskDO createTask(Long id, String code, Long loomId, Long weavingOrderId,
                                     LocalDate start, LocalDate end, String status, Integer qty) {
        MachineTaskDO task = new MachineTaskDO();
        task.setId(id);
        task.setCode(code);
        task.setLoomId(loomId);
        task.setWeavingOrderId(weavingOrderId);
        task.setPlannedStart(start);
        task.setPlannedEnd(end);
        task.setStatus(status);
        task.setPlannedQty(qty);
        task.setActualQty(0);
        return task;
    }

    private WeavingOrderDO createOrder(Long id, String code, String productCode, String productName,
                                       Integer plannedQty, String status) {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(id);
        order.setCode(code);
        order.setProductCode(productCode);
        order.setProductName(productName);
        order.setPlannedQty(plannedQty);
        order.setStatus(status);
        return order;
    }

}

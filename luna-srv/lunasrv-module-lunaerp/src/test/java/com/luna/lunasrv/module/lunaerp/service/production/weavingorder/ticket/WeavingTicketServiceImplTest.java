package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link WeavingTicketServiceImpl} 单元测试
 *
 * 覆盖 Issue 05 核心规格：
 * 1. 布票表 + 按机台任务批量生成（事务行锁、余量校验、单匹重量带入）；
 * 2. BT 派生编号规则（BT-{机台任务号去符号}-{4 位流水}，按织造单计数，作废占号不重用，撞键重试）；
 * 3. 开工/完工/异常/作废白名单守卫（111/112/113）；
 * 4. 三层状态同事务推导（任务 actualQty/状态、织造单状态，含 HALTED 不覆盖、作废收敛自然完工）；
 * 5. 并发冲突重试与保护。
 *
 * @author luna
 */
class WeavingTicketServiceImplTest {

    @Mock
    private WeavingTicketMapper weavingTicketMapper;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @InjectMocks
    private WeavingTicketServiceImpl weavingTicketService;

    private WeavingOrderDO defaultOrder;
    private MachineTaskDO defaultTask;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeavingTicketDO.class);

        defaultOrder = new WeavingOrderDO();
        defaultOrder.setId(100L);
        defaultOrder.setCode("ZZ-PB0919001-01");
        defaultOrder.setStatus("DRAFT");
        defaultOrder.setPlannedQty(100);
        defaultOrder.setPlannedWeight(new BigDecimal("2500.00"));

        defaultTask = new MachineTaskDO();
        defaultTask.setId(200L);
        defaultTask.setCode("MT-ZZPB091900101-01");
        defaultTask.setWeavingOrderId(100L);
        defaultTask.setLoomId(10L);
        defaultTask.setLoomNo("LM20260001");
        defaultTask.setWorkerUserId(12L);
        defaultTask.setWorkerName("张三");
        defaultTask.setPlannedQty(10);
        defaultTask.setActualQty(0);
        defaultTask.setStatus("PENDING");

        when(weavingOrderMapper.selectById(100L)).thenReturn(defaultOrder);
        when(machineTaskMapper.selectOneForUpdate(any(LambdaQueryWrapper.class))).thenReturn(defaultTask);
        when(machineTaskMapper.selectById(200L)).thenReturn(defaultTask);
    }

    // ==================== 1. 批量生成与余量校验 ====================

    @Test
    void testCreateTickets_success_generatesTicketsWithCorrectFields() {
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(3)
                .materialBatchNo("MB-202609-01")
                .build();

        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L)).thenReturn(Collections.emptyList());
        when(weavingTicketMapper.selectByCodeIncludingDeleted(any())).thenReturn(null);

        List<WeavingTicketRespVO> results = weavingTicketService.createTickets(req);

        assertEquals(3, results.size());
        assertEquals("BT-MTZZPB09190010101-0001", results.get(0).getCode());
        assertEquals("BT-MTZZPB09190010101-0002", results.get(1).getCode());
        assertEquals("BT-MTZZPB09190010101-0003", results.get(2).getCode());

        // 验证单匹重量：2500.00 / 100 = 25.00
        assertEquals(new BigDecimal("25.00"), results.get(0).getPlannedWeight());
        // 验证机台快照
        assertEquals("LM20260001", results.get(0).getPlannedMachine());
        // 验证工人快照继承自任务
        assertEquals(12L, results.get(0).getWorkerUserId());
        assertEquals("张三", results.get(0).getWorkerName());
        assertEquals("MB-202609-01", results.get(0).getMaterialBatchNo());
        assertEquals("GENERATED", results.get(0).getStatus());

        verify(weavingTicketMapper, times(3)).insert(any(WeavingTicketDO.class));
    }

    @Test
    void testCreateTickets_exceedPlannedQty_throws() {
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(11) // 超过任务 plannedQty 10
                .build();

        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> weavingTicketService.createTickets(req));
        assertEquals(WEAVING_TICKET_EXCEED_PLANNED_QTY.getCode(), ex.getCode());
    }

    @Test
    void testCreateTickets_exceedRemainingQty_throws() {
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(5)
                .build();

        // 已生成 6 匹有效布票，余量仅剩 4 匹，申请 5 匹超量
        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(6L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> weavingTicketService.createTickets(req));
        assertEquals(WEAVING_TICKET_EXCEED_PLANNED_QTY.getCode(), ex.getCode());
    }

    @Test
    void testCreateTickets_taskNotExists_throws() {
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(999L)
                .count(1)
                .build();

        when(machineTaskMapper.selectOneForUpdate(any(LambdaQueryWrapper.class))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> weavingTicketService.createTickets(req));
        assertEquals(MACHINE_TASK_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCreateTickets_completedTask_throws() {
        defaultTask.setStatus("COMPLETED");
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(1)
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> weavingTicketService.createTickets(req));
        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCreateTickets_cancelledOrder_throws() {
        defaultOrder.setStatus("CANCELLED");
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(1)
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> weavingTicketService.createTickets(req));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCreateTickets_plannedWeightNull_whenOrderHasNoWeight() {
        defaultOrder.setPlannedWeight(null);
        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(1)
                .build();

        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L)).thenReturn(Collections.emptyList());

        List<WeavingTicketRespVO> results = weavingTicketService.createTickets(req);
        assertNull(results.get(0).getPlannedWeight());
    }

    // ==================== 2. BT 派生编号规则与4位流水 ====================

    @Test
    void testCreateTickets_orderLevelSequence_continuesAcrossTasks() {
        // 同一织造单下，任务 1 已生成流水 0001, 0002, 0003
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L))
                .thenReturn(Arrays.asList("BT-MTZZPB09190010101-0001", "BT-MTZZPB09190010101-0002", "BT-MTZZPB09190010101-0003"));
        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 任务 2 编号 MT-ZZPB0919001-02（去符号 MTZZPB091900102）
        defaultTask.setCode("MT-ZZPB0919001-02");

        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(2)
                .build();

        List<WeavingTicketRespVO> results = weavingTicketService.createTickets(req);

        assertEquals("BT-MTZZPB091900102-0004", results.get(0).getCode());
        assertEquals("BT-MTZZPB091900102-0005", results.get(1).getCode());
    }

    @Test
    void testCreateTickets_voidOccupiesSequence_neverReused() {
        // 票 0002 虽已作废，但仍在全量编号中，最高号为 0003
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L))
                .thenReturn(Arrays.asList("BT-MTZZPB09190010101-0001", "BT-MTZZPB09190010101-0002", "BT-MTZZPB09190010101-0003"));
        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L); // 1张作废，有效票2张

        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(1)
                .build();

        List<WeavingTicketRespVO> results = weavingTicketService.createTickets(req);

        // 新票必须从 0004 开始，绝不重用 0002
        assertEquals("BT-MTZZPB09190010101-0004", results.get(0).getCode());
    }

    @Test
    void testCreateTickets_collisionRetry_skipsOccupiedNumber() {
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L))
                .thenReturn(Collections.emptyList());
        when(weavingTicketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 模拟 BT-MTZZPB09190010101-0001 发生并发撞键已存在，0002 不存在
        WeavingTicketDO collisionDO = new WeavingTicketDO();
        collisionDO.setCode("BT-MTZZPB09190010101-0001");
        when(weavingTicketMapper.selectByCodeIncludingDeleted("BT-MTZZPB09190010101-0001")).thenReturn(collisionDO);
        when(weavingTicketMapper.selectByCodeIncludingDeleted("BT-MTZZPB09190010101-0002")).thenReturn(null);

        WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                .machineTaskId(200L)
                .count(1)
                .build();

        List<WeavingTicketRespVO> results = weavingTicketService.createTickets(req);
        assertEquals("BT-MTZZPB09190010101-0002", results.get(0).getCode());
    }

    @Test
    void testCreateTickets_concurrentGeneration_threadSafe() throws InterruptedException {
        int threadCount = 4;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger generatedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        defaultTask.setPlannedQty(20);
        when(weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(100L)).thenReturn(Collections.emptyList());
        when(weavingTicketMapper.selectCount(any())).thenReturn(0L);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    WeavingTicketCreateReqVO req = WeavingTicketCreateReqVO.builder()
                            .machineTaskId(200L)
                            .count(2)
                            .build();
                    List<WeavingTicketRespVO> res = weavingTicketService.createTickets(req);
                    generatedCount.addAndGet(res.size());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "多线程并发生成布票不应抛出异常: " + exceptions);
        assertEquals(8, generatedCount.get());
    }

    // ==================== 3. 扫码白名单守卫与异常类型校验 ====================

    @Test
    void testStartTicket_success_fromGenerated() {
        WeavingTicketDO ticket = buildTicket(1L, "GENERATED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder()
                .id(1L)
                .actualMachine("LM20260002")
                .workerUserId(15L)
                .workerName("李四")
                .build();

        weavingTicketService.startTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("STARTED", updated.getStatus());
        assertEquals("LM20260002", updated.getActualMachine());
        assertEquals(15L, updated.getWorkerUserId());
        assertEquals("李四", updated.getWorkerName());
        assertNotNull(updated.getStartedAt());
    }

    @Test
    void testStartTicket_success_fromStarted_rescanAllowed() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        ticket.setStartedAt(LocalDateTime.now().minusHours(1));
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder()
                .id(1L)
                .actualMachine("LM20260003")
                .build();

        weavingTicketService.startTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("STARTED", updated.getStatus());
        assertEquals("LM20260003", updated.getActualMachine());
    }

    @Test
    void testStartTicket_success_fromException_resumesStarted() {
        WeavingTicketDO ticket = buildTicket(1L, "EXCEPTION");
        ticket.setExceptionType("YARN_BREAK");
        ticket.setExceptionNote("断纱处理完毕");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder()
                .id(1L)
                .build();

        weavingTicketService.startTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("STARTED", updated.getStatus());
        assertNull(updated.getExceptionType());
        assertNull(updated.getExceptionNote());
    }

    @Test
    void testStartTicket_fromCompleted_throws111() {
        WeavingTicketDO ticket = buildTicket(1L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder().id(1L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.startTicket(req));
        assertEquals(WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testStartTicket_fromVoid_throws112() {
        WeavingTicketDO ticket = buildTicket(1L, "VOID");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder().id(1L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.startTicket(req));
        assertEquals(WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
    }

    @Test
    void testCompleteTicket_success_fromStarted() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketCompleteReqVO req = WeavingTicketCompleteReqVO.builder().id(1L).build();
        weavingTicketService.completeTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("COMPLETED", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void testCompleteTicket_fromGenerated_throws111() {
        WeavingTicketDO ticket = buildTicket(1L, "GENERATED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketCompleteReqVO req = WeavingTicketCompleteReqVO.builder().id(1L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.completeTicket(req));
        assertEquals(WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCompleteTicket_fromCompleted_throws111() {
        WeavingTicketDO ticket = buildTicket(1L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketCompleteReqVO req = WeavingTicketCompleteReqVO.builder().id(1L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.completeTicket(req));
        assertEquals(WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCompleteTicket_fromVoid_throws112() {
        WeavingTicketDO ticket = buildTicket(1L, "VOID");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketCompleteReqVO req = WeavingTicketCompleteReqVO.builder().id(1L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.completeTicket(req));
        assertEquals(WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
    }

    @Test
    void testExceptionTicket_success_fromGeneratedAndStarted() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketExceptionReqVO req = WeavingTicketExceptionReqVO.builder()
                .id(1L)
                .exceptionType("YARN_BREAK")
                .exceptionNote("纱筒脱落")
                .build();

        weavingTicketService.exceptionTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("EXCEPTION", updated.getStatus());
        assertEquals("YARN_BREAK", updated.getExceptionType());
        assertEquals("纱筒脱落", updated.getExceptionNote());
    }

    @Test
    void testExceptionTicket_invalidType_throws113() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketExceptionReqVO req = WeavingTicketExceptionReqVO.builder()
                .id(1L)
                .exceptionType("INVALID_TYPE")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.exceptionTicket(req));
        assertEquals(WEAVING_TICKET_EXCEPTION_TYPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void testExceptionTicket_fromCompleted_throws111() {
        WeavingTicketDO ticket = buildTicket(1L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketExceptionReqVO req = WeavingTicketExceptionReqVO.builder()
                .id(1L)
                .exceptionType("YARN_BREAK")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.exceptionTicket(req));
        assertEquals(WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testExceptionTicket_fromVoid_throws112() {
        WeavingTicketDO ticket = buildTicket(1L, "VOID");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketExceptionReqVO req = WeavingTicketExceptionReqVO.builder()
                .id(1L)
                .exceptionType("YARN_BREAK")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.exceptionTicket(req));
        assertEquals(WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
    }

    @Test
    void testVoidTicket_success_fromGeneratedStartedException() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketVoidReqVO req = WeavingTicketVoidReqVO.builder()
                .id(1L)
                .voidReason("现场误开工重打布票")
                .build();

        weavingTicketService.voidTicket(req);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        WeavingTicketDO updated = captor.getValue();
        assertEquals("VOID", updated.getStatus());
        assertEquals("现场误开工重打布票", updated.getVoidReason());
        assertNotNull(updated.getVoidedAt());
    }

    @Test
    void testVoidTicket_fromCompleted_throws111() {
        WeavingTicketDO ticket = buildTicket(1L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketVoidReqVO req = WeavingTicketVoidReqVO.builder()
                .id(1L)
                .voidReason("想作废已完工")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.voidTicket(req));
        assertEquals(WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testVoidTicket_fromAlreadyVoid_throws112() {
        WeavingTicketDO ticket = buildTicket(1L, "VOID");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketVoidReqVO req = WeavingTicketVoidReqVO.builder()
                .id(1L)
                .voidReason("重复作废")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.voidTicket(req));
        assertEquals(WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
    }

    @Test
    void testVoidTicket_emptyReason_throws() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketVoidReqVO req = WeavingTicketVoidReqVO.builder()
                .id(1L)
                .voidReason("   ")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.voidTicket(req));
        assertEquals(WEAVING_TICKET_VOID_REASON_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void testTicketNotExists_throws110() {
        when(weavingTicketMapper.selectById(999L)).thenReturn(null);

        WeavingTicketStartReqVO req = WeavingTicketStartReqVO.builder().id(999L).build();
        ServiceException ex = assertThrows(ServiceException.class, () -> weavingTicketService.startTicket(req));
        assertEquals(WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 4. 三层状态同事务自动推导（含 HALTED 保护与作废自然完工） ====================

    @Test
    void testDeriveState_ticketStart_derivesTaskInProgressAndOrderActive() {
        WeavingTicketDO ticket1 = buildTicket(1L, "STARTED");
        WeavingTicketDO ticket2 = buildTicket(2L, "GENERATED");
        List<WeavingTicketDO> tickets = Arrays.asList(ticket1, ticket2);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);

        weavingTicketService.deriveState(200L, 100L);

        // 验证任务被更新为 IN_PROGRESS，actualQty 为 0
        ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(taskCaptor.capture());
        MachineTaskDO updatedTask = taskCaptor.getValue();
        assertEquals("IN_PROGRESS", updatedTask.getStatus());
        assertEquals(0, updatedTask.getActualQty());

        // 验证织造单被更新为 ACTIVE
        ArgumentCaptor<WeavingOrderDO> orderCaptor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).updateById(orderCaptor.capture());
        WeavingOrderDO updatedOrder = orderCaptor.getValue();
        assertEquals("ACTIVE", updatedOrder.getStatus());
    }

    @Test
    void testDeriveState_allTicketsCompleted_derivesTaskAndOrderCompleted() {
        defaultTask.setPlannedQty(3);
        defaultTask.setStatus("COMPLETED");
        WeavingTicketDO ticket1 = buildTicket(1L, "COMPLETED");
        WeavingTicketDO ticket2 = buildTicket(2L, "INSPECTED");
        WeavingTicketDO ticket3 = buildTicket(3L, "STOCKED");
        List<WeavingTicketDO> tickets = Arrays.asList(ticket1, ticket2, ticket3);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);
        when(machineTaskMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(defaultTask));

        weavingTicketService.deriveState(200L, 100L);

        // 验证任务更新为 COMPLETED，actualQty = 3
        ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(taskCaptor.capture());
        MachineTaskDO updatedTask = taskCaptor.getValue();
        assertEquals("COMPLETED", updatedTask.getStatus());
        assertEquals(3, updatedTask.getActualQty());

        // 验证织造单更新为 COMPLETED
        ArgumentCaptor<WeavingOrderDO> orderCaptor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).updateById(orderCaptor.capture());
        WeavingOrderDO updatedOrder = orderCaptor.getValue();
        assertEquals("COMPLETED", updatedOrder.getStatus());
    }

    @Test
    void testDeriveState_partialGeneratedBatchCompleted_keepsTaskInProgress() {
        // 任务 plannedQty=10，第一批仅生成并完工 3 匹，仍有余量未生成，任务不能早熟为 COMPLETED
        defaultTask.setPlannedQty(10);
        WeavingTicketDO ticket1 = buildTicket(1L, "COMPLETED");
        WeavingTicketDO ticket2 = buildTicket(2L, "COMPLETED");
        WeavingTicketDO ticket3 = buildTicket(3L, "COMPLETED");
        List<WeavingTicketDO> tickets = Arrays.asList(ticket1, ticket2, ticket3);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);

        weavingTicketService.deriveState(200L, 100L);

        ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(taskCaptor.capture());
        MachineTaskDO updatedTask = taskCaptor.getValue();
        assertEquals("IN_PROGRESS", updatedTask.getStatus());
        assertEquals(3, updatedTask.getActualQty());
    }

    @Test
    void testDeriveState_taskHalted_updatesActualQty_doesNotOverwriteStatus() {
        defaultTask.setStatus("HALTED"); // 处于停机状态
        defaultTask.setPlannedQty(2);

        WeavingTicketDO ticket1 = buildTicket(1L, "COMPLETED");
        WeavingTicketDO ticket2 = buildTicket(2L, "COMPLETED");
        List<WeavingTicketDO> tickets = Arrays.asList(ticket1, ticket2);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);
        MachineTaskDO taskCopy = new MachineTaskDO();
        taskCopy.setStatus("COMPLETED");
        when(machineTaskMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(taskCopy));

        weavingTicketService.deriveState(200L, 100L);

        // 验证任务只更新了 actualQty=2，状态依然为 HALTED（不被覆盖）
        ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(taskCaptor.capture());
        MachineTaskDO updatedTask = taskCaptor.getValue();
        assertNull(updatedTask.getStatus()); // 或保持 HALTED，未更新为 COMPLETED
        assertEquals(2, updatedTask.getActualQty());

        // 织造单全部票已完工正常进入 COMPLETED
        ArgumentCaptor<WeavingOrderDO> orderCaptor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).updateById(orderCaptor.capture());
        assertEquals("COMPLETED", orderCaptor.getValue().getStatus());
    }

    @Test
    void testDeriveState_voidRemainingTickets_naturallyCompletes() {
        // ADR-0024: 现场尾单不再织，作废剩余有效票后自然完工
        defaultTask.setPlannedQty(3);
        defaultTask.setStatus("COMPLETED");
        WeavingTicketDO ticket1 = buildTicket(1L, "COMPLETED");
        WeavingTicketDO ticket2 = buildTicket(2L, "COMPLETED");
        WeavingTicketDO ticket3 = buildTicket(3L, "VOID"); // 作废票不计入有效票
        List<WeavingTicketDO> tickets = Arrays.asList(ticket1, ticket2, ticket3);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);
        when(machineTaskMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(defaultTask));

        weavingTicketService.deriveState(200L, 100L);

        // 2 张有效票全部完工且总票量已达计划量 -> 自然完工
        ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(taskCaptor.capture());
        assertEquals("COMPLETED", taskCaptor.getValue().getStatus());
        assertEquals(2, taskCaptor.getValue().getActualQty());

        ArgumentCaptor<WeavingOrderDO> orderCaptor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).updateById(orderCaptor.capture());
        assertEquals("COMPLETED", orderCaptor.getValue().getStatus());
    }

    @Test
    void testDeriveState_cancelledOrder_notOverwritten() {
        defaultOrder.setStatus("CANCELLED");

        WeavingTicketDO ticket1 = buildTicket(1L, "COMPLETED");
        List<WeavingTicketDO> tickets = Collections.singletonList(ticket1);

        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(tickets);
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(tickets);

        weavingTicketService.deriveState(200L, 100L);

        // CANCELLED 为终态，织造单绝不被 COMPLETED 覆盖
        verify(weavingOrderMapper, never()).updateById(any(WeavingOrderDO.class));
    }

    // ==================== 5. 详情与分页查询 ====================

    @Test
    void testGetTicket_success() {
        WeavingTicketDO ticket = buildTicket(1L, "GENERATED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        WeavingTicketRespVO resp = weavingTicketService.getTicket(1L);
        assertNotNull(resp);
        assertEquals("BT-MTZZPB09190010101-0001", resp.getCode());
    }

    @Test
    void testGetTicketByCode_success() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectByCode("BT-MTZZPB09190010101-0001")).thenReturn(ticket);

        WeavingTicketRespVO resp = weavingTicketService.getTicketByCode("BT-MTZZPB09190010101-0001");
        assertNotNull(resp);
        assertEquals("STARTED", resp.getStatus());
    }

    @Test
    void testGetTicketPage_success() {
        WeavingTicketPageReqVO req = new WeavingTicketPageReqVO();
        req.setMachineTaskId(200L);

        List<WeavingTicketDO> list = Collections.singletonList(buildTicket(1L, "GENERATED"));
        PageResult<WeavingTicketDO> pageResult = new PageResult<>(list, 1L);
        when(weavingTicketMapper.selectPage(req)).thenReturn(pageResult);

        PageResult<WeavingTicketRespVO> page = weavingTicketService.getTicketPage(req);
        assertEquals(1, page.getList().size());
        assertEquals(1L, page.getTotal());
    }

    // ==================== 6. 检验与入库回写 ====================

    @Test
    void testMarkInspected_success() {
        WeavingTicketDO ticket = buildTicket(1L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);
        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(Collections.singletonList(ticket));
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(ticket));
        when(machineTaskMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(defaultTask));

        weavingTicketService.markInspected(1L);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        assertEquals("INSPECTED", captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    void testMarkInspected_alreadyInspectedOrStocked_idempotent() {
        WeavingTicketDO ticket = buildTicket(1L, "INSPECTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        weavingTicketService.markInspected(1L);

        verify(weavingTicketMapper, never()).updateById(any(WeavingTicketDO.class));
    }

    @Test
    void testMarkInspected_notCompleted_noOp() {
        WeavingTicketDO ticket = buildTicket(1L, "STARTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        weavingTicketService.markInspected(1L);

        verify(weavingTicketMapper, never()).updateById(any(WeavingTicketDO.class));
    }

    @Test
    void testRollbackInspected_success() {
        WeavingTicketDO ticket = buildTicket(1L, "INSPECTED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);
        when(weavingTicketMapper.selectListByMachineTaskId(200L)).thenReturn(Collections.singletonList(ticket));
        when(weavingTicketMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(ticket));
        when(machineTaskMapper.selectListByWeavingOrderId(100L)).thenReturn(Collections.singletonList(defaultTask));

        weavingTicketService.rollbackInspected(1L);

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper).updateById(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getStatus());
    }

    @Test
    void testRollbackInspected_notInspected_noOp() {
        WeavingTicketDO ticket = buildTicket(1L, "STOCKED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket);

        weavingTicketService.rollbackInspected(1L);

        verify(weavingTicketMapper, never()).updateById(any(WeavingTicketDO.class));
    }

    @Test
    void testMarkStocked_success() {
        WeavingTicketDO ticket1 = buildTicket(1L, "INSPECTED");
        WeavingTicketDO ticket2 = buildTicket(2L, "COMPLETED");
        when(weavingTicketMapper.selectById(1L)).thenReturn(ticket1);
        when(weavingTicketMapper.selectById(2L)).thenReturn(ticket2);

        weavingTicketService.markStocked(Arrays.asList(1L, 2L));

        ArgumentCaptor<WeavingTicketDO> captor = ArgumentCaptor.forClass(WeavingTicketDO.class);
        verify(weavingTicketMapper, times(2)).updateById(captor.capture());
        List<WeavingTicketDO> updatedList = captor.getAllValues();
        assertEquals("STOCKED", updatedList.get(0).getStatus());
        assertEquals("STOCKED", updatedList.get(1).getStatus());
    }

    // ==================== 辅助方法 ====================

    private WeavingTicketDO buildTicket(Long id, String status) {
        return WeavingTicketDO.builder()
                .id(id)
                .code("BT-MTZZPB09190010101-0001")
                .weavingOrderId(100L)
                .machineTaskId(200L)
                .plannedMachine("LM20260001")
                .plannedWeight(new BigDecimal("25.00"))
                .workerUserId(12L)
                .workerName("张三")
                .status(status)
                .build();
    }

}

package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
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
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link WorkshopH5ServiceImpl} 单元测试
 *
 * @author luna
 */
class WorkshopH5ServiceImplTest {

    @InjectMocks
    private WorkshopH5ServiceImpl workshopH5Service;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private WeavingTicketMapper weavingTicketMapper;

    @Mock
    private WeavingTicketService weavingTicketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoTestHelper.initTableInfo(MachineTaskDO.class, WeavingOrderDO.class, WeavingTicketDO.class);
    }

    @Test
    @DisplayName("my-tasks: 正常获取我的机台任务分页，附带织造单快照与布票状态计数（total 排除作废票）")
    void testGetMyTasksPage_success() {
        Long workerUserId = 1001L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            // 1. Mock 机台任务数据
            MachineTaskDO task1 = MachineTaskDO.builder()
                    .id(10L)
                    .code("MT-ZZ20261021-01")
                    .weavingOrderId(200L)
                    .loomId(1L)
                    .loomNo("LM01")
                    .workerUserId(workerUserId)
                    .workerName("挡车工甲")
                    .plannedQty(20)
                    .actualQty(5)
                    .status("IN_PROGRESS")
                    .plannedStart(LocalDate.of(2026, 10, 20))
                    .plannedEnd(LocalDate.of(2026, 10, 25))
                    .build();

            MachineTaskDO task2 = MachineTaskDO.builder()
                    .id(11L)
                    .code("MT-ZZ20261021-02")
                    .weavingOrderId(201L)
                    .loomId(2L)
                    .loomNo("LM02")
                    .workerUserId(workerUserId)
                    .workerName("挡车工甲")
                    .plannedQty(10)
                    .actualQty(0)
                    .status("PENDING")
                    .build();

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(new PageResult<>(List.of(task1, task2), 2L));

            // 2. Mock 织造单批量快照
            WeavingOrderDO order1 = new WeavingOrderDO();
            order1.setId(200L);
            order1.setCode("ZZ-20261021-01");
            order1.setProductName("40支精梳纯棉汗布");
            order1.setProductCode("PRD-COTTON-40S");

            WeavingOrderDO order2 = new WeavingOrderDO();
            order2.setId(201L);
            order2.setCode("ZZ-20261021-02");
            order2.setProductName("32支涤棉混纺布");
            order2.setProductCode("PRD-TC-32S");

            when(weavingOrderMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(order1, order2));

            // 3. Mock 布票投影统计（含 VOID 作废票和 STOCKED 入库票）
            WeavingTicketDO ticket1 = WeavingTicketDO.builder().machineTaskId(10L).status("GENERATED").build();
            WeavingTicketDO ticket2 = WeavingTicketDO.builder().machineTaskId(10L).status("STARTED").build();
            WeavingTicketDO ticket3 = WeavingTicketDO.builder().machineTaskId(10L).status("COMPLETED").build();
            WeavingTicketDO ticket4 = WeavingTicketDO.builder().machineTaskId(10L).status("STOCKED").build();
            WeavingTicketDO ticketVoid = WeavingTicketDO.builder().machineTaskId(10L).status("VOID").build();
            WeavingTicketDO ticket5 = WeavingTicketDO.builder().machineTaskId(11L).status("EXCEPTION").build();

            when(weavingTicketMapper.selectList(any(LambdaQueryWrapperX.class)))
                    .thenReturn(List.of(ticket1, ticket2, ticket3, ticket4, ticketVoid, ticket5));

            // 4. 调用
            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            reqVO.setPageNo(1);
            reqVO.setPageSize(10);

            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);

            // 5. 校验结果
            assertNotNull(result);
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getList().size());

            WorkshopMyTaskRespVO vo1 = result.getList().get(0);
            assertEquals(10L, vo1.getId());
            assertEquals("MT-ZZ20261021-01", vo1.getCode());
            assertEquals("ZZ-20261021-01", vo1.getWeavingOrderCode());
            assertEquals("40支精梳纯棉汗布", vo1.getProductName());
            assertEquals("PRD-COTTON-40S", vo1.getProductCode());
            assertEquals("LM01", vo1.getLoomNo());
            assertEquals(20, vo1.getPlannedQty());
            assertEquals(5, vo1.getActualQty());
            assertEquals("IN_PROGRESS", vo1.getStatus());

            assertNotNull(vo1.getTicketCounts());
            // total 严格排除 VOID 票：1 GENERATED + 1 STARTED + 1 COMPLETED + 1 STOCKED = 4
            assertEquals(4, vo1.getTicketCounts().getTotal());
            assertEquals(1, vo1.getTicketCounts().getGenerated());
            assertEquals(1, vo1.getTicketCounts().getStarted());
            assertEquals(1, vo1.getTicketCounts().getCompleted());
            assertEquals(1, vo1.getTicketCounts().getStocked());
            assertEquals(1, vo1.getTicketCounts().getVoided());
            assertEquals(0, vo1.getTicketCounts().getInspected());

            WorkshopMyTaskRespVO vo2 = result.getList().get(1);
            assertEquals(11L, vo2.getId());
            assertEquals("ZZ-20261021-02", vo2.getWeavingOrderCode());
            assertEquals(1, vo2.getTicketCounts().getTotal());
            assertEquals(1, vo2.getTicketCounts().getException());

            // 6. 验证查询条件使用了当前登录人硬过滤
            ArgumentCaptor<LambdaQueryWrapperX> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
            verify(machineTaskMapper).selectPage(eq(reqVO), queryCaptor.capture());
            String sql = queryCaptor.getValue().getCustomSqlSegment();
            assertNotNull(sql);
        }
    }

    @Test
    @DisplayName("my-tasks: 自定义状态参数过滤")
    void testGetMyTasksPage_withCustomStatus() {
        Long workerUserId = 1002L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(PageResult.empty(0L));

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            reqVO.setStatus("IN_PROGRESS, HALTED");

            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);
            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());

            verify(weavingOrderMapper, never()).selectBatchIds(any());
            verify(weavingTicketMapper, never()).selectList(any());
        }
    }

    @Test
    @DisplayName("my-tasks: 未登录时抛出 401 UNAUTHORIZED")
    void testGetMyTasksPage_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getMyTasksPage(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("my-tasks: 任务列表为空时正确短路返回")
    void testGetMyTasksPage_emptyResult() {
        Long workerUserId = 1003L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(PageResult.empty(0L));

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());
            verifyNoInteractions(weavingOrderMapper);
            verifyNoInteractions(weavingTicketMapper);
        }
    }

    // ==================== ticket/get-by-code 扫码定位端点测试 ====================

    @Test
    @DisplayName("get-by-code: 未登录时抛出 401 UNAUTHORIZED")
    void testGetTicketByCode_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getTicketByCode("BT-001"));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("get-by-code: code 为空时抛出 039-001 布票不存在或已作废")
    void testGetTicketByCode_blankCode() {
        Long workerUserId = 1001L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getTicketByCode("   "));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("get-by-code: 布票不存在时抛出 039-001 布票不存在或已作废")
    void testGetTicketByCode_notExists() {
        Long workerUserId = 1001L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getTicketByCode("BT-NOT-EXISTS"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("get-by-code: 布票已作废(VOID)时抛出 039-001 布票不存在或已作废")
    void testGetTicketByCode_voidTicket() {
        Long workerUserId = 1001L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            WeavingTicketDO voidTicket = WeavingTicketDO.builder()
                    .id(101L)
                    .code("BT-VOID-001")
                    .status("VOID")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(voidTicket);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getTicketByCode("BT-VOID-001"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("get-by-code: 横向越权-机台任务已派给其他挡车工时抛出 039-002")
    void testGetTicketByCode_assignedToOtherWorker() {
        Long loginUserId = 1001L;
        Long otherWorkerUserId = 1002L;

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(101L)
                    .code("BT-OTHER-001")
                    .weavingOrderId(201L)
                    .machineTaskId(301L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(301L)
                    .code("MT-001")
                    .workerUserId(otherWorkerUserId)
                    .workerName("李四")
                    .build();
            when(machineTaskMapper.selectById(301L)).thenReturn(task);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getTicketByCode("BT-OTHER-001"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("get-by-code: 成功-未派工任务返回 claimable=true 且 workerUserId=null")
    void testGetTicketByCode_unassignedClaimable() {
        Long loginUserId = 1001L;

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(102L)
                    .code("BT-UNASSIGNED-001")
                    .weavingOrderId(201L)
                    .machineTaskId(302L)
                    .plannedMachine("LM02")
                    .plannedWeight(new BigDecimal("25.00"))
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(302L)
                    .code("MT-002")
                    .loomId(2L)
                    .loomNo("LM02")
                    .workerUserId(null)
                    .workerName(null)
                    .build();
            when(machineTaskMapper.selectById(302L)).thenReturn(task);

            WeavingOrderDO order = new WeavingOrderDO();
            order.setId(201L);
            order.setCode("ZZ-2026-01");
            order.setProductCode("PRD-01");
            order.setProductName("40S 纯棉单面汗布");
            order.setComposition("100% 棉");
            order.setGsm("180g/m²");
            order.setWidth("185cm");
            when(weavingOrderMapper.selectById(201L)).thenReturn(order);

            WorkshopTicketRespVO resp = workshopH5Service.getTicketByCode("BT-UNASSIGNED-001");

            assertNotNull(resp);
            assertEquals(102L, resp.getId());
            assertEquals("BT-UNASSIGNED-001", resp.getCode());
            assertEquals("GENERATED", resp.getStatus());
            assertEquals("ZZ-2026-01", resp.getWeavingOrderCode());
            assertEquals("MT-002", resp.getMachineTaskCode());
            assertEquals("LM02", resp.getLoomNo());
            assertEquals("40S 纯棉单面汗布", resp.getProductName());
            assertNull(resp.getWorkerUserId());
            assertNull(resp.getWorkerName());
            assertTrue(resp.getClaimable());
        }
    }

    @Test
    @DisplayName("get-by-code: 成功-已派工给登录人返回 claimable=true 且携带挡车工快照")
    void testGetTicketByCode_success_assignedToSelf() {
        Long loginUserId = 1001L;

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(103L)
                    .code("BT-SELF-001")
                    .weavingOrderId(201L)
                    .machineTaskId(303L)
                    .plannedMachine("LM03")
                    .plannedWeight(new BigDecimal("26.50"))
                    .status("STARTED")
                    .materialBatchNo("YB2026-01")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(303L)
                    .code("MT-003")
                    .loomId(3L)
                    .loomNo("LM03")
                    .workerUserId(loginUserId)
                    .workerName("张三")
                    .build();
            when(machineTaskMapper.selectById(303L)).thenReturn(task);

            WeavingOrderDO order = new WeavingOrderDO();
            order.setId(201L);
            order.setCode("ZZ-2026-01");
            order.setProductCode("PRD-01");
            order.setProductName("40S 纯棉单面汗布");
            when(weavingOrderMapper.selectById(201L)).thenReturn(order);

            WorkshopTicketRespVO resp = workshopH5Service.getTicketByCode("BT-SELF-001");

            assertNotNull(resp);
            assertEquals(103L, resp.getId());
            assertEquals("BT-SELF-001", resp.getCode());
            assertEquals("STARTED", resp.getStatus());
            assertEquals(loginUserId, resp.getWorkerUserId());
            assertEquals("张三", resp.getWorkerName());
            assertEquals("YB2026-01", resp.getMaterialBatchNo());
            assertTrue(resp.getClaimable());
        }
    }

    // ==================== ticket/start 单元测试 ====================

    @Test
    @DisplayName("startTicket: 未登录工人调用抛出 401 UNAUTHORIZED")
    void testStartTicket_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                    .code("BT-TEST-001")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("startTicket: 布票编号为空抛出 039-001")
    void testStartTicket_blankCode() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                    .code("   ")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("startTicket: 布票不存在或已作废(VOID)抛出 039-001")
    void testStartTicket_notExistsOrVoid() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            // 1. 布票查无此票
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);
            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder().code("BT-NONE").build();
            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex1.getCode());

            // 2. 布票已作废
            WeavingTicketDO voidTicket = WeavingTicketDO.builder().id(999L).code("BT-VOID").status("VOID").build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(voidTicket);
            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("startTicket: 横向越权-工人 A 操作工人 B 的机台任务抛出 039-002")
    void testStartTicket_assignedToOther_throws039_002() {
        Long loginUserId = 1001L;
        Long otherWorkerUserId = 2002L;

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(101L)
                    .code("BT-OTHER-001")
                    .machineTaskId(301L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(301L)
                    .workerUserId(otherWorkerUserId)
                    .workerName("李四")
                    .build();
            when(machineTaskMapper.selectById(301L)).thenReturn(task);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder().code("BT-OTHER-001").build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode(), ex.getCode());
            verify(weavingTicketService, never()).startTicket(any());
        }
    }

    @Test
    @DisplayName("startTicket: 成功-已派工给登录人直接开工，workerUserId/workerName强制取登录人")
    void testStartTicket_success_assignedToSelf() {
        Long loginUserId = 1001L;
        String loginNickname = "张三";

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);
            securityMock.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn(loginNickname);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(102L)
                    .code("BT-SELF-002")
                    .machineTaskId(302L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(302L)
                    .workerUserId(loginUserId)
                    .workerName("张三")
                    .build();
            when(machineTaskMapper.selectById(302L)).thenReturn(task);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                    .code("BT-SELF-002")
                    .actualMachine("LM08")
                    .build();

            workshopH5Service.startTicket(reqVO);

            // 验证未发生 machine_task 条件更新（已属于登录人）
            verify(machineTaskMapper, never()).update(any(), any());

            // 验证委托生产域 weavingTicketService.startTicket，且强制取登录人身份
            ArgumentCaptor<WeavingTicketStartReqVO> captor = ArgumentCaptor.forClass(WeavingTicketStartReqVO.class);
            verify(weavingTicketService).startTicket(captor.capture());
            WeavingTicketStartReqVO passed = captor.getValue();
            assertEquals(102L, passed.getId());
            assertEquals("LM08", passed.getActualMachine());
            assertEquals(loginUserId, passed.getWorkerUserId());
            assertEquals(loginNickname, passed.getWorkerName());
        }
    }

    @Test
    @DisplayName("startTicket: 成功-未派工任务自助认领+开工（条件更新 WHERE worker_user_id IS NULL 成功）")
    void testStartTicket_success_selfClaim() {
        Long loginUserId = 1001L;
        String loginNickname = "张三";

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);
            securityMock.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn(loginNickname);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(103L)
                    .code("BT-UNASSIGNED-003")
                    .machineTaskId(303L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(303L)
                    .workerUserId(null)
                    .workerName(null)
                    .build();
            when(machineTaskMapper.selectById(303L)).thenReturn(task);

            // 条件更新命中 1 行，代表成功认领
            when(machineTaskMapper.update(any(MachineTaskDO.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                    .code("BT-UNASSIGNED-003")
                    .build();

            workshopH5Service.startTicket(reqVO);

            // 验证条件更新写为登录人
            ArgumentCaptor<MachineTaskDO> taskCaptor = ArgumentCaptor.forClass(MachineTaskDO.class);
            verify(machineTaskMapper).update(taskCaptor.capture(), any(LambdaUpdateWrapper.class));
            assertEquals(loginUserId, taskCaptor.getValue().getWorkerUserId());
            assertEquals(loginNickname, taskCaptor.getValue().getWorkerName());

            // 验证委托生产域 weavingTicketService.startTicket
            ArgumentCaptor<WeavingTicketStartReqVO> ticketCaptor = ArgumentCaptor.forClass(WeavingTicketStartReqVO.class);
            verify(weavingTicketService).startTicket(ticketCaptor.capture());
            assertEquals(103L, ticketCaptor.getValue().getId());
            assertEquals(loginUserId, ticketCaptor.getValue().getWorkerUserId());
            assertEquals(loginNickname, ticketCaptor.getValue().getWorkerName());
            assertNull(ticketCaptor.getValue().getActualMachine());
        }
    }

    @Test
    @DisplayName("startTicket: 并发认领落败-条件更新返回 0 抛出 039-002")
    void testStartTicket_selfClaim_concurrencyLoss_throws039_002() {
        Long loginUserId = 1001L;

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(104L)
                    .code("BT-RACE-004")
                    .machineTaskId(304L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(304L)
                    .workerUserId(null)
                    .build();
            when(machineTaskMapper.selectById(304L)).thenReturn(task);

            // 模拟并发更新落败：已有其他工人抢先认领，更新行数为 0
            when(machineTaskMapper.update(any(MachineTaskDO.class), any(LambdaUpdateWrapper.class))).thenReturn(0);

            WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder().code("BT-RACE-004").build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.startTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode(), ex.getCode());
            verify(weavingTicketService, never()).startTicket(any());
        }
    }

    @Test
    @DisplayName("startTicket: 切面注解标注 @LogRecord 留痕")
    void testStartTicket_hasLogRecordAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = WorkshopH5ServiceImpl.class.getMethod("startTicket", WorkshopTicketStartReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "startTicket 必须标注 @LogRecord 切面注解");
        assertEquals("车间移动H5", logRecord.type());
        assertEquals("布票开工", logRecord.subType());
        assertEquals("{{#reqVO.code}}", logRecord.bizNo());
    }

}

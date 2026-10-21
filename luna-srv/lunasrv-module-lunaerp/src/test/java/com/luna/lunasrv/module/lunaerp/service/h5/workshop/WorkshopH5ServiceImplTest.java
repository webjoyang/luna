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
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketScanRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item.InspectionOrderItemService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderService;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderHandoffInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants;
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
import java.util.Collections;
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

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Mock
    private WeavingOrderService weavingOrderService;

    @Mock
    private InspectionOrderItemService inspectionOrderItemService;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoTestHelper.initTableInfo(
                MachineTaskDO.class,
                WeavingOrderDO.class,
                WeavingTicketDO.class,
                InspectionOrderDO.class,
                InspectionOrderItemDO.class,
                GreigeOrderDO.class,
                GreigeOrderColorDO.class
        );
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

    // ==================== ticket/complete 单元测试 ====================

    @Test
    @DisplayName("completeTicket: 未登录工人调用抛出 401 UNAUTHORIZED")
    void testCompleteTicket_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-001").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("completeTicket: 布票编号为空抛出 039-001")
    void testCompleteTicket_blankCode() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("   ").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("completeTicket: 布票不存在抛出 039-001")
    void testCompleteTicket_notExists() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-NONE").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("completeTicket: VOID 作废票抛出既有守卫码 015-112")
    void testCompleteTicket_voidTicket_throws015_112() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO voidTicket = WeavingTicketDO.builder().id(999L).code("BT-VOID").status("VOID").build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(voidTicket);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-VOID").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(ErrorCodeConstants.WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("completeTicket: 横向越权-工人 A 操作工人 B 的机台任务抛出 039-002")
    void testCompleteTicket_assignedToOther_throws039_002() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(101L)
                    .code("BT-OTHER-001")
                    .machineTaskId(301L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(301L)
                    .workerUserId(2002L)
                    .build();
            when(machineTaskMapper.selectById(301L)).thenReturn(task);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-OTHER-001").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode(), ex.getCode());
            verify(weavingTicketService, never()).completeTicket(any());
        }
    }

    @Test
    @DisplayName("completeTicket: 幂等友好-票已 COMPLETED 再调 complete 返回成功 alreadyCompleted=true")
    void testCompleteTicket_alreadyCompleted_fromCompleted() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(102L)
                    .code("BT-COMP-002")
                    .machineTaskId(302L)
                    .status("COMPLETED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder()
                    .id(302L)
                    .workerUserId(1001L)
                    .build();
            when(machineTaskMapper.selectById(302L)).thenReturn(task);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-COMP-002").build();
            WorkshopTicketCompleteRespVO respVO = workshopH5Service.completeTicket(reqVO);

            assertNotNull(respVO);
            assertTrue(respVO.getAlreadyCompleted());
            verify(weavingTicketService, never()).completeTicket(any());
        }
    }

    @Test
    @DisplayName("completeTicket: 幂等友好-票已 INSPECTED / STOCKED 返回成功 alreadyCompleted=true")
    void testCompleteTicket_alreadyCompleted_fromInspectedAndStocked() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            // 1. INSPECTED
            WeavingTicketDO inspectedTicket = WeavingTicketDO.builder()
                    .id(103L)
                    .code("BT-INSP-003")
                    .machineTaskId(303L)
                    .status("INSPECTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(inspectedTicket);
            MachineTaskDO task = MachineTaskDO.builder().id(303L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(303L)).thenReturn(task);

            WorkshopTicketCompleteReqVO reqVO1 = WorkshopTicketCompleteReqVO.builder().code("BT-INSP-003").build();
            WorkshopTicketCompleteRespVO resp1 = workshopH5Service.completeTicket(reqVO1);
            assertTrue(resp1.getAlreadyCompleted());

            // 2. STOCKED
            WeavingTicketDO stockedTicket = WeavingTicketDO.builder()
                    .id(104L)
                    .code("BT-STK-004")
                    .machineTaskId(303L)
                    .status("STOCKED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(stockedTicket);

            WorkshopTicketCompleteReqVO reqVO2 = WorkshopTicketCompleteReqVO.builder().code("BT-STK-004").build();
            WorkshopTicketCompleteRespVO resp2 = workshopH5Service.completeTicket(reqVO2);
            assertTrue(resp2.getAlreadyCompleted());

            verify(weavingTicketService, never()).completeTicket(any());
        }
    }

    @Test
    @DisplayName("completeTicket: 非 STARTED 来源态（如 GENERATED）抛出既有守卫码 015-111")
    void testCompleteTicket_statusNotAllow_throws015_111() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(105L)
                    .code("BT-GEN-005")
                    .machineTaskId(305L)
                    .status("GENERATED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(305L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(305L)).thenReturn(task);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-GEN-005").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.completeTicket(reqVO));
            assertEquals(ErrorCodeConstants.WEAVING_TICKET_STATUS_NOT_ALLOW.getCode(), ex.getCode());
            verify(weavingTicketService, never()).completeTicket(any());
        }
    }

    @Test
    @DisplayName("completeTicket: 成功-从 STARTED 完工，委托 Service 并返回 alreadyCompleted=false")
    void testCompleteTicket_success_fromStarted() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(106L)
                    .code("BT-START-006")
                    .machineTaskId(306L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(306L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(306L)).thenReturn(task);

            WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder().code("BT-START-006").build();
            WorkshopTicketCompleteRespVO resp = workshopH5Service.completeTicket(reqVO);

            assertNotNull(resp);
            assertFalse(resp.getAlreadyCompleted());

            ArgumentCaptor<WeavingTicketCompleteReqVO> captor = ArgumentCaptor.forClass(WeavingTicketCompleteReqVO.class);
            verify(weavingTicketService).completeTicket(captor.capture());
            assertEquals(106L, captor.getValue().getId());
            assertNotNull(captor.getValue().getCompletedAt());
        }
    }

    @Test
    @DisplayName("completeTicket: 切面注解标注 @LogRecord 留痕")
    void testCompleteTicket_hasLogRecordAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = WorkshopH5ServiceImpl.class.getMethod("completeTicket", WorkshopTicketCompleteReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "completeTicket 必须标注 @LogRecord 切面注解");
        assertEquals("车间移动H5", logRecord.type());
        assertEquals("布票完工", logRecord.subType());
        assertEquals("{{#reqVO.code}}", logRecord.bizNo());
    }

    // ==================== ticket/exception 单元测试 ====================

    @Test
    @DisplayName("exceptionTicket: 未登录工人调用抛出 401 UNAUTHORIZED")
    void testExceptionTicket_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder().code("BT-EX-001").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 布票编号为空抛出 039-001")
    void testExceptionTicket_blankCode() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder().code("   ").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 布票不存在抛出 039-001")
    void testExceptionTicket_notExists() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);

            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder().code("BT-EX-NONE").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("exceptionTicket: VOID 作废票抛出既有守卫码 015-112")
    void testExceptionTicket_voidTicket_throws015_112() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO voidTicket = WeavingTicketDO.builder().id(999L).code("BT-VOID").status("VOID").build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(voidTicket);

            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder().code("BT-VOID").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(ErrorCodeConstants.WEAVING_TICKET_ALREADY_VOIDED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 横向越权-工人 A 操作工人 B 的机台任务抛出 039-002")
    void testExceptionTicket_assignedToOther_throws039_002() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(107L)
                    .code("BT-OTHER-007")
                    .machineTaskId(307L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(307L).workerUserId(2002L).build();
            when(machineTaskMapper.selectById(307L)).thenReturn(task);

            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder().code("BT-OTHER-007").build();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode(), ex.getCode());
            verify(weavingTicketService, never()).exceptionTicket(any());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 照片守卫-超过 6 张照片抛出 039-008")
    void testExceptionTicket_photoCountExceed_throws039_008() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(108L)
                    .code("BT-EX-008")
                    .machineTaskId(308L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(308L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(308L)).thenReturn(task);

            List<String> sevenPhotos = List.of(
                    "https://oss.example.com/p1.jpg",
                    "https://oss.example.com/p2.jpg",
                    "https://oss.example.com/p3.jpg",
                    "https://oss.example.com/p4.jpg",
                    "https://oss.example.com/p5.jpg",
                    "https://oss.example.com/p6.jpg",
                    "https://oss.example.com/p7.jpg"
            );
            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder()
                    .code("BT-EX-008")
                    .exceptionType("YARN_BREAK")
                    .exceptionPhotos(sevenPhotos)
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode(), ex.getCode());
            verify(weavingTicketService, never()).exceptionTicket(any());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 照片守卫-含非图片 URL（如 PDF / 纯文本）抛出 039-008")
    void testExceptionTicket_invalidPhotoUrl_throws039_008() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(109L)
                    .code("BT-EX-009")
                    .machineTaskId(309L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(309L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(309L)).thenReturn(task);

            // 1. pdf 文件
            WorkshopTicketExceptionReqVO reqVO1 = WorkshopTicketExceptionReqVO.builder()
                    .code("BT-EX-009")
                    .exceptionType("YARN_BREAK")
                    .exceptionPhotos(List.of("https://oss.example.com/doc.pdf"))
                    .build();
            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO1));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode(), ex1.getCode());

            // 2. 非 URL 纯文本
            WorkshopTicketExceptionReqVO reqVO2 = WorkshopTicketExceptionReqVO.builder()
                    .code("BT-EX-009")
                    .exceptionType("YARN_BREAK")
                    .exceptionPhotos(List.of("not-a-valid-url"))
                    .build();
            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.exceptionTicket(reqVO2));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode(), ex2.getCode());

            verify(weavingTicketService, never()).exceptionTicket(any());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 成功-委托 Service 登记异常（传递 id, exceptionType, exceptionNote, exceptionPhotos）")
    void testExceptionTicket_success_persistsPhotosAndOverwrites() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(110L)
                    .code("BT-EX-010")
                    .machineTaskId(310L)
                    .status("STARTED")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            MachineTaskDO task = MachineTaskDO.builder().id(310L).workerUserId(1001L).build();
            when(machineTaskMapper.selectById(310L)).thenReturn(task);

            List<String> photos = List.of(
                    "https://oss.example.com/p1.jpg",
                    "https://oss.example.com/p2.png",
                    "/admin-api/infra/file/get/p3.webp"
            );
            WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder()
                    .code("BT-EX-010")
                    .exceptionType("YARN_BREAK")
                    .exceptionNote("3号经纱断头")
                    .exceptionPhotos(photos)
                    .build();

            workshopH5Service.exceptionTicket(reqVO);

            ArgumentCaptor<WeavingTicketExceptionReqVO> captor = ArgumentCaptor.forClass(WeavingTicketExceptionReqVO.class);
            verify(weavingTicketService).exceptionTicket(captor.capture());
            WeavingTicketExceptionReqVO captured = captor.getValue();
            assertEquals(110L, captured.getId());
            assertEquals("YARN_BREAK", captured.getExceptionType());
            assertEquals("3号经纱断头", captured.getExceptionNote());
            assertEquals(photos, captured.getExceptionPhotos());
        }
    }

    @Test
    @DisplayName("exceptionTicket: 切面注解标注 @LogRecord 留痕")
    void testExceptionTicket_hasLogRecordAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = WorkshopH5ServiceImpl.class.getMethod("exceptionTicket", WorkshopTicketExceptionReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "exceptionTicket 必须标注 @LogRecord 切面注解");
        assertEquals("车间移动H5", logRecord.type());
        assertEquals("布票异常登记", logRecord.subType());
        assertEquals("{{#reqVO.code}}", logRecord.bizNo());
    }

    // ==========================================
    // 移动质检单分页与详情单测（Issue 13）
    // ==========================================

    @Test
    @DisplayName("getInspectionPage: 未登录抛出 UNAUTHORIZED (401)")
    void testGetInspectionPage_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopInspectionPageReqVO reqVO = new WorkshopInspectionPageReqVO();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getInspectionPage(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("getInspectionPage: 缺省过滤 DRAFT/ACTIVE 状态，批量统计 rollCount 进度")
    void testGetInspectionPage_defaultStatusAndBatchProgress() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order1 = new InspectionOrderDO();
            order1.setId(101L);
            order1.setCode("INS-20261021-001");
            order1.setType("GREIGE");
            order1.setStatus("ACTIVE");
            order1.setPackageMode("PIECE");
            order1.setProductId(51L);
            order1.setProductCode("P001");
            order1.setProductName("全棉汗布");
            order1.setInspectionFactoryName("一号车间");
            order1.setOrderDate(LocalDate.of(2026, 10, 21));
            order1.setTargetWeight(new BigDecimal("500.00"));

            InspectionOrderDO order2 = new InspectionOrderDO();
            order2.setId(102L);
            order2.setCode("INS-20261021-002");
            order2.setType("FINISHED");
            order2.setStatus("DRAFT");
            order2.setPackageMode("ROLL");
            order2.setProductId(52L);
            order2.setProductCode("P002");
            order2.setProductName("斜纹布");
            order2.setInspectionFactoryName("二号车间");
            order2.setOrderDate(LocalDate.of(2026, 10, 21));

            PageResult<InspectionOrderDO> pageDO = new PageResult<>(List.of(order1, order2), 2L);
            when(inspectionOrderMapper.selectPage(any(WorkshopInspectionPageReqVO.class), any())).thenReturn(pageDO);
            when(inspectionOrderItemMapper.selectCountMapByInspectionOrderIds(List.of(101L, 102L)))
                    .thenReturn(java.util.Map.of(101L, 8, 102L, 0));

            WorkshopInspectionPageReqVO reqVO = new WorkshopInspectionPageReqVO();
            PageResult<WorkshopInspectionRespVO> result = workshopH5Service.getInspectionPage(reqVO);

            assertNotNull(result);
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getList().size());

            WorkshopInspectionRespVO vo1 = result.getList().get(0);
            assertEquals(101L, vo1.getId());
            assertEquals("INS-20261021-001", vo1.getCode());
            assertEquals("ACTIVE", vo1.getStatus());
            assertEquals("GREIGE", vo1.getType());
            assertEquals(8, vo1.getRollCount());

            WorkshopInspectionRespVO vo2 = result.getList().get(1);
            assertEquals(102L, vo2.getId());
            assertEquals("INS-20261021-002", vo2.getCode());
            assertEquals("DRAFT", vo2.getStatus());
            assertEquals(0, vo2.getRollCount());
        }
    }

    @Test
    @DisplayName("getInspectionPage: 空列表时返回空分页")
    void testGetInspectionPage_empty() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            when(inspectionOrderMapper.selectPage(any(WorkshopInspectionPageReqVO.class), any()))
                    .thenReturn(PageResult.empty(0L));

            WorkshopInspectionPageReqVO reqVO = new WorkshopInspectionPageReqVO();
            PageResult<WorkshopInspectionRespVO> result = workshopH5Service.getInspectionPage(reqVO);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());
            verify(inspectionOrderItemMapper, never()).selectCountMapByInspectionOrderIds(any());
        }
    }

    @Test
    @DisplayName("getInspection: 未登录抛出 UNAUTHORIZED (401)")
    void testGetInspection_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getInspection(101L));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("getInspection: 单据不存在或跨租户隔离落空抛 010-000")
    void testGetInspection_notExists() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            when(inspectionOrderMapper.selectById(999L)).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getInspection(999L));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("getInspection: CANCELLED 状态不允许进入抛 010-002")
    void testGetInspection_cancelled() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(103L);
            order.setStatus("CANCELLED");
            when(inspectionOrderMapper.selectById(103L)).thenReturn(order);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getInspection(103L));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("getInspection: ACTIVE 状态可进可改 (readonly=false)，包含表头与 photos 明细行")
    void testGetInspection_active_editable() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setCode("INS-20261021-001");
            order.setType("GREIGE");
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            order.setProductCode("P001");
            order.setProductName("全棉汗布");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            InspectionOrderItemDO item1 = new InspectionOrderItemDO();
            item1.setId(501L);
            item1.setInspectionOrderId(101L);
            item1.setPieceNo("BT-001");
            item1.setGrade("A");
            item1.setMeasuredWeight(new BigDecimal("25.30"));
            item1.setPhotos(List.of("https://oss.example.com/p1.jpg", "https://oss.example.com/p2.jpg"));

            when(inspectionOrderItemMapper.selectList(any())).thenReturn(List.of(item1));

            WorkshopInspectionDetailRespVO detail = workshopH5Service.getInspection(101L);

            assertNotNull(detail);
            assertEquals(101L, detail.getId());
            assertEquals("INS-20261021-001", detail.getCode());
            assertEquals("ACTIVE", detail.getStatus());
            assertFalse(detail.getReadonly(), "ACTIVE 态应为可改状态 (readonly=false)");
            assertEquals(1, detail.getRollCount());
            assertEquals(1, detail.getItems().size());
            assertEquals("BT-001", detail.getItems().get(0).getPieceNo());
            assertEquals(2, detail.getItems().get(0).getPhotos().size());
            assertEquals("https://oss.example.com/p1.jpg", detail.getItems().get(0).getPhotos().get(0));
        }
    }

    @Test
    @DisplayName("getInspection: COMPLETED 状态进入为只读模式 (readonly=true)")
    void testGetInspection_completed_readonly() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(102L);
            order.setCode("INS-20261021-002");
            order.setType("FINISHED");
            order.setStatus("COMPLETED");
            order.setPackageMode("ROLL");
            order.setProductCode("P002");
            order.setProductName("涤纶斜纹");
            when(inspectionOrderMapper.selectById(102L)).thenReturn(order);

            when(inspectionOrderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

            WorkshopInspectionDetailRespVO detail = workshopH5Service.getInspection(102L);

            assertNotNull(detail);
            assertEquals(102L, detail.getId());
            assertEquals("COMPLETED", detail.getStatus());
            assertTrue(detail.getReadonly(), "COMPLETED 态必须为只读模式 (readonly=true)");
            assertEquals(0, detail.getRollCount());
            assertTrue(detail.getItems().isEmpty());
        }
    }

    // ================== ticketScan (Issue 14 胚布扫码定位编排) ==================

    @Test
    @DisplayName("ticketScan: 未登录抛 UNAUTHORIZED")
    void testTicketScan_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-001"));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: code 为空抛 039-001")
    void testTicketScan_blankCode() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan(null));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex1.getCode());

            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("   "));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: 布票不存在或已作废(VOID)抛 039-001")
    void testTicketScan_ticketNotExistsOrVoid() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // 1. 布票不存在
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);
            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-NOT-EXISTS"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex1.getCode());

            // 2. 布票已作废 (VOID)
            WeavingTicketDO voidTicket = WeavingTicketDO.builder()
                    .id(201L)
                    .code("BT-VOID-001")
                    .status("VOID")
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(voidTicket);
            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-VOID-001"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: 布票尚未完工(GENERATED/STARTED)抛 039-003")
    void testTicketScan_ticketNotCompleted() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // 1. GENERATED
            WeavingTicketDO genTicket = WeavingTicketDO.builder()
                    .id(201L)
                    .code("BT-GEN-001")
                    .status("GENERATED")
                    .weavingOrderId(101L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(genTicket);
            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-GEN-001"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_TICKET_NOT_COMPLETED.getCode(), ex1.getCode());

            // 2. STARTED
            WeavingTicketDO startTicket = WeavingTicketDO.builder()
                    .id(202L)
                    .code("BT-START-002")
                    .status("STARTED")
                    .weavingOrderId(101L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(startTicket);
            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-START-002"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_TICKET_NOT_COMPLETED.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: 布票其余不可录态(EXCEPTION)抛 039-004")
    void testTicketScan_ticketStatusNotAllow() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            WeavingTicketDO excTicket = WeavingTicketDO.builder()
                    .id(203L)
                    .code("BT-EXC-003")
                    .status("EXCEPTION")
                    .weavingOrderId(101L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(excTicket);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-EXC-003"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_TICKET_STATUS_NOT_ALLOW_INSPECTION.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: 首扫完工票自动交接建单，返回检验单ID+布票建议匹号，item 为 null")
    void testTicketScan_firstScan_autoHandoff_noExistingItem() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(301L)
                    .code("BT-COMP-001")
                    .status("COMPLETED")
                    .weavingOrderId(501L)
                    .workerUserId(9999L) // 挡车工非当前质检员
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            // Mock handoffInspection 返回创建的检验单 601L
            WeavingOrderHandoffInspectionRespVO handoffResp = WeavingOrderHandoffInspectionRespVO.builder()
                    .inspectionOrderId(601L)
                    .inspectionOrderCode("INS-20261021-001")
                    .created(true)
                    .reused(false)
                    .build();
            when(weavingOrderService.handoffInspection(501L)).thenReturn(handoffResp);

            // Mock 检验单为 DRAFT
            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(601L);
            order.setStatus("DRAFT");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(order);

            // Mock 录检明细尚无记录
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.emptyList());

            WorkshopTicketScanRespVO result = workshopH5Service.ticketScan("BT-COMP-001");

            assertNotNull(result);
            assertEquals(601L, result.getInspectionOrderId());
            assertEquals(301L, result.getWeavingTicketId());
            assertEquals("BT-COMP-001", result.getSuggestedPieceNo());
            assertNull(result.getItem(), "首扫尚未录检，item 应为 null");
            verify(weavingOrderService).handoffInspection(501L);
        }
    }

    @Test
    @DisplayName("ticketScan: 连扫两次同一票（重放同票），复用同一检验单，已录检明细存在返回 item 供改录")
    void testTicketScan_rescanSameTicket_hasExistingItem() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(301L)
                    .code("BT-COMP-001")
                    .status("INSPECTED")
                    .weavingOrderId(501L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            // Mock handoffInspection 幂等复用检验单 601L
            WeavingOrderHandoffInspectionRespVO handoffResp = WeavingOrderHandoffInspectionRespVO.builder()
                    .inspectionOrderId(601L)
                    .created(false)
                    .reused(true)
                    .build();
            when(weavingOrderService.handoffInspection(501L)).thenReturn(handoffResp);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(601L);
            order.setStatus("ACTIVE");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(order);

            // Mock 该布票已录检的明细行
            InspectionOrderItemDO existingItem = new InspectionOrderItemDO();
            existingItem.setId(801L);
            existingItem.setInspectionOrderId(601L);
            existingItem.setWeavingTicketId(301L);
            existingItem.setPieceNo("BT-COMP-001");
            existingItem.setGrade("A");
            existingItem.setMeasuredWeight(new BigDecimal("26.80"));
            existingItem.setLengthM(new BigDecimal("105.00"));
            existingItem.setPhotos(List.of("https://oss.example.com/item1.jpg"));
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(existingItem));

            WorkshopTicketScanRespVO result = workshopH5Service.ticketScan("BT-COMP-001");

            assertNotNull(result);
            assertEquals(601L, result.getInspectionOrderId());
            assertEquals(301L, result.getWeavingTicketId());
            assertNull(result.getSuggestedPieceNo(), "重放已录检票，Spec 规定仅返回 {inspectionOrderId, item}，suggestedPieceNo 为 null");
            assertNotNull(result.getItem(), "重放已录检票，应返回已有 item 供改录");
            assertEquals(801L, result.getItem().getId());
            assertEquals("A", result.getItem().getGrade());
            assertEquals(new BigDecimal("26.80"), result.getItem().getMeasuredWeight());
            assertEquals(1, result.getItem().getPhotos().size());
        }
    }

    @Test
    @DisplayName("ticketScan: 连扫同织造单第二票，复用同一检验单、不重复建单，未录检返回 item=null")
    void testTicketScan_secondTicket_sameOrder_noItem() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            WeavingTicketDO ticket2 = WeavingTicketDO.builder()
                    .id(302L)
                    .code("BT-COMP-002")
                    .status("COMPLETED")
                    .weavingOrderId(501L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket2);

            // 同织造单，交接返回相同检验单 601L
            WeavingOrderHandoffInspectionRespVO handoffResp = WeavingOrderHandoffInspectionRespVO.builder()
                    .inspectionOrderId(601L)
                    .created(false)
                    .reused(true)
                    .build();
            when(weavingOrderService.handoffInspection(501L)).thenReturn(handoffResp);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(601L);
            order.setStatus("ACTIVE");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(order);

            // 第二票未录检
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.emptyList());

            WorkshopTicketScanRespVO result = workshopH5Service.ticketScan("BT-COMP-002");

            assertNotNull(result);
            assertEquals(601L, result.getInspectionOrderId());
            assertEquals(302L, result.getWeavingTicketId());
            assertEquals("BT-COMP-002", result.getSuggestedPieceNo());
            assertNull(result.getItem());
        }
    }

    @Test
    @DisplayName("ticketScan: 该检验单已 COMPLETED/CANCELLED 抛 010-002")
    void testTicketScan_inspectionOrder_completedOrCancelled() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(301L)
                    .code("BT-COMP-001")
                    .status("COMPLETED")
                    .weavingOrderId(501L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            WeavingOrderHandoffInspectionRespVO handoffResp = WeavingOrderHandoffInspectionRespVO.builder()
                    .inspectionOrderId(601L)
                    .build();
            when(weavingOrderService.handoffInspection(501L)).thenReturn(handoffResp);

            // 1. 已 COMPLETED
            InspectionOrderDO completedOrder = new InspectionOrderDO();
            completedOrder.setId(601L);
            completedOrder.setStatus("COMPLETED");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(completedOrder);

            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-COMP-001"));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

            // 2. 已 CANCELLED
            InspectionOrderDO cancelledOrder = new InspectionOrderDO();
            cancelledOrder.setId(601L);
            cancelledOrder.setStatus("CANCELLED");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(cancelledOrder);

            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-COMP-001"));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("ticketScan: 质检不受机台任务挡车工归属限制，任意质检员均可扫码交接定位")
    void testTicketScan_noWorkerRestriction_inspectorCanScan() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            // 当前登录质检员为 7777L
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7777L);

            // 布票上的挡车工为 8888L，两者不同
            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(301L)
                    .code("BT-COMP-001")
                    .status("COMPLETED")
                    .weavingOrderId(501L)
                    .workerUserId(8888L)
                    .build();
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(ticket);

            WeavingOrderHandoffInspectionRespVO handoffResp = WeavingOrderHandoffInspectionRespVO.builder()
                    .inspectionOrderId(601L)
                    .created(false)
                    .reused(true)
                    .build();
            when(weavingOrderService.handoffInspection(501L)).thenReturn(handoffResp);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(601L);
            order.setStatus("ACTIVE");
            when(inspectionOrderMapper.selectById(601L)).thenReturn(order);
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.emptyList());

            // 不受机台任务挡车工归属校验影响，顺利返回
            WorkshopTicketScanRespVO result = workshopH5Service.ticketScan("BT-COMP-001");
            assertNotNull(result);
            assertEquals(601L, result.getInspectionOrderId());
        }
    }

    @Test
    @DisplayName("ticketScan: 跨租户布票隔离，查无布票抛 039-001")
    void testTicketScan_crossTenant_isolated() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // MyBatis-Plus 租户插件自动附加 tenant_id 过滤，跨租户查询返回 null
            when(weavingTicketMapper.selectOne(any(LambdaQueryWrapperX.class))).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.ticketScan("BT-OTHER-TENANT"));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    // ==================== inspection/item/save 单元测试 ====================

    @Test
    @DisplayName("saveInspectionItem: 未登录用户调用抛出 401 UNAUTHORIZED")
    void testSaveInspectionItem_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 检验单不存在抛出 010-000")
    void testSaveInspectionItem_orderNotExists() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            when(inspectionOrderMapper.selectById(999L)).thenReturn(null);

            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(999L)
                    .weavingTicketId(201L)
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 检验单处于 COMPLETED 或 CANCELLED 状态抛出 010-002（改录仅允许 DRAFT/ACTIVE）")
    void testSaveInspectionItem_orderStatusNotAllowed() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // 1. COMPLETED
            InspectionOrderDO completedOrder = new InspectionOrderDO();
            completedOrder.setId(101L);
            completedOrder.setStatus("COMPLETED");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(completedOrder);

            WorkshopInspectionItemSaveReqVO reqVO1 = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .build();

            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO1));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

            // 2. CANCELLED
            InspectionOrderDO cancelledOrder = new InspectionOrderDO();
            cancelledOrder.setId(102L);
            cancelledOrder.setStatus("CANCELLED");
            when(inspectionOrderMapper.selectById(102L)).thenReturn(cancelledOrder);

            WorkshopInspectionItemSaveReqVO reqVO2 = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(102L)
                    .weavingTicketId(201L)
                    .build();

            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO2));
            assertEquals(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 照片守卫 7 张超过 6 张限制抛出 039-008")
    void testSaveInspectionItem_photosExceed_039_008() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            List<String> sevenPhotos = List.of(
                    "https://oss.example.com/1.jpg",
                    "https://oss.example.com/2.jpg",
                    "https://oss.example.com/3.jpg",
                    "https://oss.example.com/4.jpg",
                    "https://oss.example.com/5.jpg",
                    "https://oss.example.com/6.jpg",
                    "https://oss.example.com/7.jpg"
            );

            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .photos(sevenPhotos)
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 照片包含非图片 URL 格式抛出 039-008")
    void testSaveInspectionItem_photoInvalidFormat_039_008() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .photos(List.of("https://oss.example.com/malicious.exe"))
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(reqVO));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 胚布 PIECE 模式下布票不存在或已作废(VOID)抛出 039-001")
    void testSaveInspectionItem_piece_ticketNotExistsOrVoid() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            // 1. ticketId 为空
            WorkshopInspectionItemSaveReqVO nullTicketReq = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(null)
                    .build();
            ServiceException ex1 = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(nullTicketReq));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex1.getCode());

            // 2. ticket 查无记录
            when(weavingTicketMapper.selectById(201L)).thenReturn(null);
            WorkshopInspectionItemSaveReqVO notExistsReq = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .build();
            ServiceException ex2 = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(notExistsReq));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex2.getCode());

            // 3. ticket 状态为 VOID
            WeavingTicketDO voidTicket = WeavingTicketDO.builder().id(202L).status("VOID").build();
            when(weavingTicketMapper.selectById(202L)).thenReturn(voidTicket);
            WorkshopInspectionItemSaveReqVO voidReq = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(202L)
                    .build();
            ServiceException ex3 = assertThrows(ServiceException.class, () -> workshopH5Service.saveInspectionItem(voidReq));
            assertEquals(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode(), ex3.getCode());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 胚布 PIECE 模式空 vatNo 正常保存，机台号/产品编码品名/颜色快照由服务端补齐，委托 createInspectionOrderItem")
    void testSaveInspectionItem_piece_greige_emptyVatNo_snapshotsAutoFilled_success() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // 1. 检验单：GREIGE, PIECE, DRAFT
            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("DRAFT");
            order.setType("GREIGE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            // 2. 布票：ticket
            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(201L)
                    .code("BT-MTZZPB09190010101-0001")
                    .status("COMPLETED")
                    .weavingOrderId(501L)
                    .actualMachine("LM-18")
                    .plannedMachine("LM-16")
                    .build();
            when(weavingTicketMapper.selectById(201L)).thenReturn(ticket);

            // 3. 织造单：weavingOrder -> greigeOrderId
            WeavingOrderDO weavingOrder = new WeavingOrderDO();
            weavingOrder.setId(501L);
            weavingOrder.setProductCode("PROD-GREIGE-01");
            weavingOrder.setProductName("纯棉精梳胚布");
            weavingOrder.setGreigeOrderId(701L);
            when(weavingOrderMapper.selectById(501L)).thenReturn(weavingOrder);

            // 4. 胚布单与颜色：greigeOrder & greigeOrderColor
            GreigeOrderDO greigeOrder = new GreigeOrderDO();
            greigeOrder.setId(701L);
            when(greigeOrderMapper.selectById(701L)).thenReturn(greigeOrder);

            GreigeOrderColorDO colorDO = new GreigeOrderColorDO();
            colorDO.setId(801L);
            colorDO.setOrderId(701L);
            colorDO.setLineNo(1);
            colorDO.setColor("本白原色");
            when(greigeOrderColorMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(colorDO));

            // 5. 单内无既有明细
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.emptyList());

            // 6. Mock 生产域 service 返回新增 ID
            when(inspectionOrderItemService.createInspectionOrderItem(any(InspectionOrderItemSaveReqVO.class))).thenReturn(9001L);

            // 客户端提交：空 vatNo，未传 pieceNo（缺省为布票号），附带照片和实测重量
            List<String> photos = List.of("https://oss.example.com/p1.jpg", "https://oss.example.com/p2.png");
            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .pieceNo(null) // 缺省布票 code
                    .vatNo(null)   // GREIGE+PIECE 允许空
                    .grade("A")
                    .measuredWeight(new java.math.BigDecimal("25.80"))
                    .lengthM(new java.math.BigDecimal("102.50"))
                    .remark("现场布面平整")
                    .photos(photos)
                    .build();

            Long savedId = workshopH5Service.saveInspectionItem(reqVO);

            assertEquals(9001L, savedId);

            // 校验委托参数完整性与快照服务端权威补齐
            ArgumentCaptor<InspectionOrderItemSaveReqVO> captor = ArgumentCaptor.forClass(InspectionOrderItemSaveReqVO.class);
            verify(inspectionOrderItemService).createInspectionOrderItem(captor.capture());
            InspectionOrderItemSaveReqVO savedReq = captor.getValue();

            assertEquals(101L, savedReq.getInspectionOrderId());
            assertEquals(201L, savedReq.getWeavingTicketId());
            assertEquals("BT-MTZZPB09190010101-0001", savedReq.getPieceNo()); // 缺省布票号
            assertNull(savedReq.getVatNo());                                  // 允许空
            assertEquals("LM-18", savedReq.getMachineNo());                   // actualMachine 补齐
            assertEquals("PROD-GREIGE-01", savedReq.getProductCode());       // 织造单补齐
            assertEquals("纯棉精梳胚布", savedReq.getProductName());          // 织造单补齐
            assertEquals("本白原色", savedReq.getColorName());                // 胚布单颜色补齐
            assertEquals("A", savedReq.getGrade());
            assertEquals(new java.math.BigDecimal("25.80"), savedReq.getMeasuredWeight());
            assertEquals(new java.math.BigDecimal("102.50"), savedReq.getLengthM());
            assertEquals("现场布面平整", savedReq.getRemark());
            assertEquals(photos, savedReq.getPhotos());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 胚布 PIECE 重扫同票改录原行，未传 id 自动识别单内既有明细，委托 updateInspectionOrderItem")
    void testSaveInspectionItem_piece_reScan_autoLocatesExistingItem_updates_success() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            // 1. 检验单 ACTIVE
            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            // 2. 布票 ticket
            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(201L)
                    .code("BT-MTZZPB09190010101-0001")
                    .status("INSPECTED")
                    .weavingOrderId(501L)
                    .actualMachine("LM-18")
                    .build();
            when(weavingTicketMapper.selectById(201L)).thenReturn(ticket);

            WeavingOrderDO weavingOrder = new WeavingOrderDO();
            weavingOrder.setId(501L);
            weavingOrder.setProductCode("PROD-01");
            weavingOrder.setProductName("纯棉布");
            when(weavingOrderMapper.selectById(501L)).thenReturn(weavingOrder);

            // 3. 单内已存在同 weavingTicketId 的明细行（ID 为 555L）
            InspectionOrderItemDO existItem = new InspectionOrderItemDO();
            existItem.setId(555L);
            existItem.setInspectionOrderId(101L);
            existItem.setWeavingTicketId(201L);
            existItem.setGrade("B");
            when(inspectionOrderItemMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(existItem));

            // 未传 id 改录
            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .id(null)
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .pieceNo("BT-MTZZPB09190010101-0001-MOD") // 改录自定义匹号
                    .grade("A")                                // 改为 A 级
                    .measuredWeight(new java.math.BigDecimal("26.00"))
                    .lengthM(new java.math.BigDecimal("105.00"))
                    .remark("复检合格改录")
                    .photos(List.of("https://oss.example.com/p3.jpg"))
                    .build();

            Long resultId = workshopH5Service.saveInspectionItem(reqVO);

            assertEquals(555L, resultId); // 返回既有行 ID

            ArgumentCaptor<InspectionOrderItemSaveReqVO> captor = ArgumentCaptor.forClass(InspectionOrderItemSaveReqVO.class);
            verify(inspectionOrderItemService).updateInspectionOrderItem(captor.capture());
            InspectionOrderItemSaveReqVO updatedReq = captor.getValue();

            assertEquals(555L, updatedReq.getId());
            assertEquals("BT-MTZZPB09190010101-0001-MOD", updatedReq.getPieceNo());
            assertEquals("A", updatedReq.getGrade());
            assertEquals(new java.math.BigDecimal("26.00"), updatedReq.getMeasuredWeight());
            assertEquals(new java.math.BigDecimal("105.00"), updatedReq.getLengthM());
            assertEquals("复检合格改录", updatedReq.getRemark());
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 显式指定 id 改录指定明细，委托 updateInspectionOrderItem 并返回该 id")
    void testSaveInspectionItem_piece_explicitId_updates_success() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2001L);

            InspectionOrderDO order = new InspectionOrderDO();
            order.setId(101L);
            order.setStatus("ACTIVE");
            order.setPackageMode("PIECE");
            when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .id(201L)
                    .code("BT-001")
                    .status("INSPECTED")
                    .weavingOrderId(501L)
                    .plannedMachine("LM-02")
                    .build();
            when(weavingTicketMapper.selectById(201L)).thenReturn(ticket);

            WeavingOrderDO weavingOrder = new WeavingOrderDO();
            weavingOrder.setId(501L);
            when(weavingOrderMapper.selectById(501L)).thenReturn(weavingOrder);

            WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                    .id(888L)
                    .inspectionOrderId(101L)
                    .weavingTicketId(201L)
                    .grade("C")
                    .build();

            Long resultId = workshopH5Service.saveInspectionItem(reqVO);

            assertEquals(888L, resultId);
            verify(inspectionOrderItemService).updateInspectionOrderItem(any(InspectionOrderItemSaveReqVO.class));
            verify(inspectionOrderItemService, never()).createInspectionOrderItem(any(InspectionOrderItemSaveReqVO.class));
        }
    }

    @Test
    @DisplayName("saveInspectionItem: 校验切面注解标注 @LogRecord(subType = '录检行保存')")
    void testSaveInspectionItem_logRecordAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = WorkshopH5ServiceImpl.class.getMethod("saveInspectionItem", WorkshopInspectionItemSaveReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "saveInspectionItem 必须标注 @LogRecord");
        assertEquals("车间移动H5", logRecord.type());
        assertEquals("录检行保存", logRecord.subType());
        assertEquals("{{#reqVO.inspectionOrderId}}", logRecord.bizNo());
    }

}

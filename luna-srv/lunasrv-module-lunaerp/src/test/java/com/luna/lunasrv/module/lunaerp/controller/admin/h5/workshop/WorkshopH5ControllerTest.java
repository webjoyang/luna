package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketScanRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionRespVO;
import com.luna.lunasrv.framework.idempotent.core.annotation.Idempotent;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionDispatchReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopWarehouseSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.service.h5.workshop.WorkshopH5Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link WorkshopH5Controller} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("车间移动 H5 - 接入层 Controller 单测")
class WorkshopH5ControllerTest {

    @Mock
    private WorkshopH5Service workshopH5Service;

    @InjectMocks
    private WorkshopH5Controller workshopH5Controller;

    @Test
    @DisplayName("ticket/start: 委托 Service 并返回 success(true)")
    void testStartTicket_success() {
        WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                .code("BT-TEST-001")
                .actualMachine("LM01")
                .build();

        CommonResult<Boolean> result = workshopH5Controller.startTicket(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(workshopH5Service).startTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/start: 校验端点注解与权限表达式")
    void testStartTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("startTicket", WorkshopTicketStartReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "startTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/start"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "startTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

    @Test
    @DisplayName("ticket/complete: 委托 Service 并返回 success(respVO)")
    void testCompleteTicket_success() {
        WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder()
                .code("BT-TEST-001")
                .build();
        WorkshopTicketCompleteRespVO respVO = WorkshopTicketCompleteRespVO.builder()
                .alreadyCompleted(Boolean.FALSE)
                .build();
        when(workshopH5Service.completeTicket(reqVO)).thenReturn(respVO);

        CommonResult<WorkshopTicketCompleteRespVO> result = workshopH5Controller.completeTicket(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().getAlreadyCompleted());
        verify(workshopH5Service).completeTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/complete: 幂等完工返回 alreadyCompleted=true")
    void testCompleteTicket_alreadyCompleted() {
        WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder()
                .code("BT-TEST-001")
                .build();
        WorkshopTicketCompleteRespVO respVO = WorkshopTicketCompleteRespVO.builder()
                .alreadyCompleted(Boolean.TRUE)
                .build();
        when(workshopH5Service.completeTicket(reqVO)).thenReturn(respVO);

        CommonResult<WorkshopTicketCompleteRespVO> result = workshopH5Controller.completeTicket(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getAlreadyCompleted());
        verify(workshopH5Service).completeTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/complete: 校验端点注解与权限表达式")
    void testCompleteTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("completeTicket", WorkshopTicketCompleteReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "completeTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/complete"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "completeTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

    @Test
    @DisplayName("ticket/exception: 委托 Service 并返回 success(true)")
    void testExceptionTicket_success() {
        WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder()
                .code("BT-TEST-001")
                .exceptionType("YARN_BREAK")
                .exceptionNote("断纱")
                .build();

        CommonResult<Boolean> result = workshopH5Controller.exceptionTicket(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(workshopH5Service).exceptionTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/exception: 校验端点注解与权限表达式")
    void testExceptionTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("exceptionTicket", WorkshopTicketExceptionReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "exceptionTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/exception"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "exceptionTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

    @Test
    @DisplayName("inspection/page: 委托 Service 并返回分页列表")
    void testGetInspectionPage_success() {
        WorkshopInspectionPageReqVO reqVO = new WorkshopInspectionPageReqVO();
        reqVO.setStatus("DRAFT,ACTIVE");
        WorkshopInspectionRespVO respVO = WorkshopInspectionRespVO.builder()
                .id(101L)
                .code("INS-20261021-001")
                .type("GREIGE")
                .status("ACTIVE")
                .rollCount(3)
                .build();
        PageResult<WorkshopInspectionRespVO> pageResult = new PageResult<>(java.util.List.of(respVO), 1L);
        when(workshopH5Service.getInspectionPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<WorkshopInspectionRespVO>> result = workshopH5Controller.getInspectionPage(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getList().size());
        assertEquals("INS-20261021-001", result.getData().getList().get(0).getCode());
        verify(workshopH5Service).getInspectionPage(reqVO);
    }

    @Test
    @DisplayName("inspection/page: 校验端点注解与权限表达式")
    void testGetInspectionPage_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getInspectionPage", WorkshopInspectionPageReqVO.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getInspectionPage 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/inspection/page"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getInspectionPage 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:inspection')", preAuthorize.value());
    }

    @Test
    @DisplayName("inspection/get: 委托 Service 并返回详情")
    void testGetInspection_success() {
        Long id = 101L;
        WorkshopInspectionDetailRespVO detailVO = WorkshopInspectionDetailRespVO.builder()
                .id(id)
                .code("INS-20261021-001")
                .type("GREIGE")
                .status("ACTIVE")
                .readonly(false)
                .items(java.util.Collections.emptyList())
                .build();
        when(workshopH5Service.getInspection(id)).thenReturn(detailVO);

        CommonResult<WorkshopInspectionDetailRespVO> result = workshopH5Controller.getInspection(id);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(id, result.getData().getId());
        assertFalse(result.getData().getReadonly());
        verify(workshopH5Service).getInspection(id);
    }

    @Test
    @DisplayName("inspection/get: 校验端点注解与权限表达式")
    void testGetInspection_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getInspection", Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getInspection 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/inspection/get"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getInspection 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:inspection')", preAuthorize.value());
    }

    @Test
    @DisplayName("inspection/ticket-scan: 委托 Service 并返回扫码定位编排结果")
    void testTicketScan_success() {
        String code = "BT-001";
        WorkshopTicketScanRespVO scanRespVO = WorkshopTicketScanRespVO.builder()
                .inspectionOrderId(101L)
                .weavingTicketId(201L)
                .suggestedPieceNo("BT-001")
                .item(null)
                .build();
        when(workshopH5Service.ticketScan(code)).thenReturn(scanRespVO);

        CommonResult<WorkshopTicketScanRespVO> result = workshopH5Controller.ticketScan(code);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(101L, result.getData().getInspectionOrderId());
        assertEquals(201L, result.getData().getWeavingTicketId());
        assertEquals("BT-001", result.getData().getSuggestedPieceNo());
        assertNull(result.getData().getItem());
        verify(workshopH5Service).ticketScan(code);
    }

    @Test
    @DisplayName("inspection/ticket-scan: 校验端点注解与权限表达式")
    void testTicketScan_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("ticketScan", String.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "ticketScan 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/inspection/ticket-scan"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "ticketScan 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:inspection')", preAuthorize.value());
    }

    @Test
    @DisplayName("inspection/item/save: 委托 Service 并返回保存的明细ID")
    void testSaveInspectionItem_success() {
        WorkshopInspectionItemSaveReqVO reqVO = WorkshopInspectionItemSaveReqVO.builder()
                .inspectionOrderId(101L)
                .weavingTicketId(201L)
                .pieceNo("BT-001")
                .grade("A")
                .measuredWeight(new BigDecimal("25.50"))
                .lengthM(new BigDecimal("100.00"))
                .build();
        when(workshopH5Service.saveInspectionItem(reqVO)).thenReturn(501L);

        CommonResult<Long> result = workshopH5Controller.saveInspectionItem(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(501L, result.getData());
        verify(workshopH5Service).saveInspectionItem(reqVO);
    }

    @Test
    @DisplayName("inspection/item/save: 校验端点注解与权限表达式")
    void testSaveInspectionItem_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("saveInspectionItem", WorkshopInspectionItemSaveReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "saveInspectionItem 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/inspection/item/save"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "saveInspectionItem 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:inspection')", preAuthorize.value());
    }

    @Test
    @DisplayName("requisition/page: 委托 Service 并返回分页数据")
    void testGetRequisitionPage_success() {
        WorkshopRequisitionPageReqVO reqVO = new WorkshopRequisitionPageReqVO();
        reqVO.setStatus("ACTIVE");
        PageResult<WorkshopRequisitionRespVO> pageResult = new PageResult<>(
                List.of(WorkshopRequisitionRespVO.builder().id(101L).code("YS-001").status("ACTIVE").build()),
                1L
        );
        when(workshopH5Service.getRequisitionPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<WorkshopRequisitionRespVO>> result = workshopH5Controller.getRequisitionPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getList().size());
        assertEquals("YS-001", result.getData().getList().get(0).getCode());
        verify(workshopH5Service).getRequisitionPage(reqVO);
    }

    @Test
    @DisplayName("requisition/page: 校验端点注解与权限表达式")
    void testGetRequisitionPage_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getRequisitionPage", WorkshopRequisitionPageReqVO.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getRequisitionPage 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/requisition/page"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getRequisitionPage 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:dispatch')", preAuthorize.value());
    }

    @Test
    @DisplayName("requisition/get: 委托 Service 并返回详情")
    void testGetRequisition_success() {
        Long id = 101L;
        WorkshopRequisitionDetailRespVO detailVO = WorkshopRequisitionDetailRespVO.builder()
                .id(id)
                .code("YS-001")
                .status("ACTIVE")
                .defaultWarehouseId(1L)
                .build();
        when(workshopH5Service.getRequisition(id)).thenReturn(detailVO);

        CommonResult<WorkshopRequisitionDetailRespVO> result = workshopH5Controller.getRequisition(id);

        assertTrue(result.isSuccess());
        assertEquals(101L, result.getData().getId());
        assertEquals("YS-001", result.getData().getCode());
        verify(workshopH5Service).getRequisition(id);
    }

    @Test
    @DisplayName("requisition/get: 校验端点注解与权限表达式")
    void testGetRequisition_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getRequisition", Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getRequisition 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/requisition/get"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getRequisition 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:dispatch')", preAuthorize.value());
    }

    @Test
    @DisplayName("warehouse/simple-list: 委托 Service 并返回仓库简表")
    void testGetWarehouseSimpleList_success() {
        List<WorkshopWarehouseSimpleRespVO> list = List.of(
                WorkshopWarehouseSimpleRespVO.builder().id(1L).name("原料仓").build()
        );
        when(workshopH5Service.getWarehouseSimpleList()).thenReturn(list);

        CommonResult<List<WorkshopWarehouseSimpleRespVO>> result = workshopH5Controller.getWarehouseSimpleList();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("原料仓", result.getData().get(0).getName());
        verify(workshopH5Service).getWarehouseSimpleList();
    }

    @Test
    @DisplayName("warehouse/simple-list: 校验端点注解与权限表达式")
    void testGetWarehouseSimpleList_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getWarehouseSimpleList");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getWarehouseSimpleList 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/warehouse/simple-list"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getWarehouseSimpleList 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:dispatch')", preAuthorize.value());
    }

    @Test
    @DisplayName("yarn-balance: 委托 Service 并返回纱线余额")
    void testGetYarnBalance_success() {
        Long warehouseId = 1L;
        Long yarnSpecId = 10L;
        BigDecimal balance = new BigDecimal("500.00");
        when(workshopH5Service.getYarnBalance(warehouseId, yarnSpecId)).thenReturn(balance);

        CommonResult<BigDecimal> result = workshopH5Controller.getYarnBalance(warehouseId, yarnSpecId);

        assertTrue(result.isSuccess());
        assertEquals(balance, result.getData());
        verify(workshopH5Service).getYarnBalance(warehouseId, yarnSpecId);
    }

    @Test
    @DisplayName("yarn-balance: 校验端点注解与权限表达式")
    void testGetYarnBalance_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("getYarnBalance", Long.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "getYarnBalance 必须标注 @GetMapping");
        assertArrayEquals(new String[]{"/yarn-balance"}, getMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getYarnBalance 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:dispatch')", preAuthorize.value());
    }

    @Test
    @DisplayName("requisition-item/dispatch: 委托 Service 并返回 success(true)")
    void testDispatchYarn_success() {
        WorkshopRequisitionDispatchReqVO reqVO = WorkshopRequisitionDispatchReqVO.builder()
                .itemId(1024L)
                .warehouseId(1L)
                .locationId(10L)
                .dispatchQty(new BigDecimal("50.00"))
                .remark("现场发纱")
                .requestId("550e8400-e29b-41d4-a716-446655440000")
                .build();

        CommonResult<Boolean> result = workshopH5Controller.dispatchYarn(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(workshopH5Service).dispatchYarn(reqVO);
    }

    @Test
    @DisplayName("requisition-item/dispatch: 校验端点注解、权限表达式与幂等配置")
    void testDispatchYarn_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("dispatchYarn", WorkshopRequisitionDispatchReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "dispatchYarn 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/requisition-item/dispatch"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "dispatchYarn 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:dispatch')", preAuthorize.value());

        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        assertNotNull(idempotent, "dispatchYarn 必须标注 @Idempotent");
        assertEquals(30, idempotent.timeout());
        assertEquals(TimeUnit.SECONDS, idempotent.timeUnit());
        assertEquals(ExpressionIdempotentKeyResolver.class, idempotent.keyResolver());
        assertEquals("#reqVO.requestId", idempotent.keyArg());
        assertEquals("发纱正在处理，请勿重复提交", idempotent.message());
    }

}

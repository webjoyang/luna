package com.luna.lunasrv.module.lunaerp.controller.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceApplicationController} 单元测试（契约与权限校验）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class InvoiceApplicationControllerTest {

    @Mock
    private InvoiceApplicationService invoiceApplicationService;

    @InjectMocks
    private InvoiceApplicationController controller;

    @Test
    @DisplayName("create 接口：委托 Service 创建发票申请草稿并返回 ID")
    void testCreateInvoiceApplication() {
        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(1001L)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();
        when(invoiceApplicationService.createInvoiceApplication(reqVO)).thenReturn(888L);

        CommonResult<Long> result = controller.createInvoiceApplication(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(888L, result.getData());
        verify(invoiceApplicationService).createInvoiceApplication(reqVO);
    }

    @Test
    @DisplayName("get 接口：委托 Service 获取发票申请详情")
    void testGetInvoiceApplication() {
        InvoiceApplicationDetailRespVO detail = new InvoiceApplicationDetailRespVO();
        detail.setId(888L);
        detail.setInvoiceNo("IA20260001");
        when(invoiceApplicationService.getInvoiceApplication(888L)).thenReturn(detail);

        CommonResult<InvoiceApplicationDetailRespVO> result = controller.getInvoiceApplication(888L);

        assertTrue(result.isSuccess());
        assertEquals(detail, result.getData());
        verify(invoiceApplicationService).getInvoiceApplication(888L);
    }

    @Test
    @DisplayName("page 接口：委托 Service 获取发票申请分页")
    void testGetInvoiceApplicationPage() {
        InvoiceApplicationPageReqVO pageReqVO = new InvoiceApplicationPageReqVO();
        PageResult<InvoiceApplicationRespVO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(invoiceApplicationService.getInvoiceApplicationPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<InvoiceApplicationRespVO>> result = controller.getInvoiceApplicationPage(pageReqVO);

        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(invoiceApplicationService).getInvoiceApplicationPage(pageReqVO);
    }

    @Test
    @DisplayName("update 接口：委托 Service 更新发票申请")
    void testUpdateInvoiceApplication() {
        InvoiceApplicationUpdateReqVO updateReqVO = InvoiceApplicationUpdateReqVO.builder()
                .id(888L)
                .taxRate(new BigDecimal("13.00"))
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();

        CommonResult<Boolean> result = controller.updateInvoiceApplication(updateReqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(invoiceApplicationService).updateInvoiceApplication(updateReqVO);
    }

    @Test
    @DisplayName("delete 接口：委托 Service 删除发票申请")
    void testDeleteInvoiceApplication() {
        CommonResult<Boolean> result = controller.deleteInvoiceApplication(888L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(invoiceApplicationService).deleteInvoiceApplication(888L);
    }

    @Test
    @DisplayName("submit 接口：委托 Service 提交发票申请")
    void testSubmitInvoiceApplication() {
        CommonResult<Boolean> result = controller.submitInvoiceApplication(888L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(invoiceApplicationService).submitInvoiceApplication(888L);
    }

    @Test
    @DisplayName("issue 接口：委托 Service 登记开票")
    void testIssueInvoiceApplication() {
        InvoiceApplicationIssueReqVO reqVO = InvoiceApplicationIssueReqVO.builder()
                .id(888L)
                .officialInvoiceNo("24322000000000000001")
                .invoiceDate(LocalDate.of(2026, 10, 18))
                .build();

        CommonResult<Boolean> result = controller.issueInvoiceApplication(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(invoiceApplicationService).issueInvoiceApplication(reqVO);
    }

    @Test
    @DisplayName("reject 接口：委托 Service 驳回发票申请")
    void testRejectInvoiceApplication() {
        InvoiceApplicationRejectReqVO reqVO = InvoiceApplicationRejectReqVO.builder()
                .id(888L)
                .rejectReason("抬头有误")
                .build();

        CommonResult<Boolean> result = controller.rejectInvoiceApplication(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(invoiceApplicationService).rejectInvoiceApplication(reqVO);
    }

    @Test
    @DisplayName("candidate-shipments 接口：委托 Service 查询候选发货单")
    void testGetCandidateShipments() {
        InvoiceCandidateShipmentRespVO shipmentResp = InvoiceCandidateShipmentRespVO.builder()
                .shipmentId(2001L)
                .shipmentNo("SH-20261018-001")
                .availableAmount(new BigDecimal("1000.00"))
                .build();
        when(invoiceApplicationService.getCandidateShipments(1001L, "RMB")).thenReturn(List.of(shipmentResp));

        CommonResult<List<InvoiceCandidateShipmentRespVO>> result = controller.getCandidateShipments(1001L, "RMB");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(shipmentResp, result.getData().get(0));
        verify(invoiceApplicationService).getCandidateShipments(1001L, "RMB");
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("createInvoiceApplication", new Class<?>[]{InvoiceApplicationCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:create')");
        assertPermission("getInvoiceApplication", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:invoice:query')");
        assertPermission("getInvoiceApplicationPage", new Class<?>[]{InvoiceApplicationPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:query')");
        assertPermission("updateInvoiceApplication", new Class<?>[]{InvoiceApplicationUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:update')");
        assertPermission("deleteInvoiceApplication", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:invoice:delete')");
        assertPermission("submitInvoiceApplication", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:invoice:update')");
        assertPermission("issueInvoiceApplication", new Class<?>[]{InvoiceApplicationIssueReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:issue')");
        assertPermission("rejectInvoiceApplication", new Class<?>[]{InvoiceApplicationRejectReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:issue')");
        assertPermission("getCandidateShipments", new Class<?>[]{Long.class, String.class}, "@ss.hasPermission('lunaerp:finance:invoice:query')");
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = InvoiceApplicationController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}

package com.luna.lunasrv.module.lunaerp.controller.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceKindEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InvoiceController} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    @Test
    @DisplayName("创建申请端点转发验证")
    void testCreateInvoice() {
        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(1001L)
                .invoiceKind(InvoiceKindEnum.ELEC_SPECIAL.getValue())
                .items(List.of(InvoiceItemSaveVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();
        when(invoiceService.createInvoice(any(InvoiceSaveReqVO.class))).thenReturn(1234L);

        CommonResult<Long> result = invoiceController.createInvoice(reqVO);
        assertNotNull(result);
        assertEquals(1234L, result.getData());
        verify(invoiceService).createInvoice(reqVO);
    }

    @Test
    @DisplayName("分页查询端点转发验证")
    void testGetInvoicePage() {
        InvoicePageReqVO pageReqVO = new InvoicePageReqVO();
        pageReqVO.setCustomerId(1001L);

        PageResult<InvoiceRespVO> expected = PageResult.empty();
        when(invoiceService.getInvoicePage(any(InvoicePageReqVO.class))).thenReturn(expected);

        CommonResult<PageResult<InvoiceRespVO>> result = invoiceController.getInvoicePage(pageReqVO);
        assertNotNull(result);
        assertEquals(expected, result.getData());
        verify(invoiceService).getInvoicePage(pageReqVO);
    }

    @Test
    @DisplayName("获取详情端点转发验证")
    void testGetInvoice() {
        InvoiceRespVO expected = InvoiceRespVO.builder().id(1234L).invoiceNo("IA20260001").build();
        when(invoiceService.getInvoice(1234L)).thenReturn(expected);

        CommonResult<InvoiceRespVO> result = invoiceController.getInvoice(1234L);
        assertNotNull(result);
        assertEquals(expected, result.getData());
        verify(invoiceService).getInvoice(1234L);
    }

    @Test
    @DisplayName("提交申请端点转发验证")
    void testSubmitInvoice() {
        CommonResult<Boolean> result = invoiceController.submitInvoice(1234L);
        assertNotNull(result);
        assertEquals(true, result.getData());
        verify(invoiceService).submitInvoice(1234L);
    }

    @Test
    @DisplayName("开票登记端点转发验证")
    void testIssueInvoice() {
        InvoiceIssueReqVO reqVO = InvoiceIssueReqVO.builder()
                .id(1234L)
                .officialInvoiceNo("24332000000012345678")
                .invoiceDate(LocalDate.now())
                .build();

        CommonResult<Boolean> result = invoiceController.issueInvoice(reqVO, null);
        assertNotNull(result);
        assertEquals(true, result.getData());
        verify(invoiceService).issueInvoice(reqVO);
    }

    @Test
    @DisplayName("驳回申请端点转发验证")
    void testRejectInvoice() {
        CommonResult<Boolean> result = invoiceController.rejectInvoice(1234L, "信息有误");
        assertNotNull(result);
        assertEquals(true, result.getData());
        verify(invoiceService).rejectInvoice(1234L, "信息有误");
    }

    @Test
    @DisplayName("删除申请端点转发验证")
    void testDeleteInvoice() {
        CommonResult<Boolean> result = invoiceController.deleteInvoice(1234L);
        assertNotNull(result);
        assertEquals(true, result.getData());
        verify(invoiceService).deleteInvoice(1234L);
    }

    @Test
    @DisplayName("候选发货单列表端点转发验证")
    void testGetCandidateShipments() {
        when(invoiceService.getCandidateShipments(1001L)).thenReturn(Collections.emptyList());

        CommonResult<List<InvoiceCandidateShipmentRespVO>> result = invoiceController.getCandidateShipments(1001L);
        assertNotNull(result);
        assertEquals(0, result.getData().size());
        verify(invoiceService).getCandidateShipments(1001L);
    }

}

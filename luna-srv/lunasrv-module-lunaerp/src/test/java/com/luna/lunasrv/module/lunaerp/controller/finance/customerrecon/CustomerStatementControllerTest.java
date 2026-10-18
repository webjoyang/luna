package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerStatementController} 单元测试（契约与权限校验）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerStatementControllerTest {

    @Mock
    private CustomerStatementService customerStatementService;

    @InjectMocks
    private CustomerStatementController controller;

    @Test
    @DisplayName("prefetchLines 接口：委托 Service 并返回预览与候选数据")
    void testPrefetchLines() {
        CustomerStatementPrefetchReqVO reqVO = new CustomerStatementPrefetchReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setCurrency("CNY");
        reqVO.setPeriodStart(LocalDate.of(2026, 10, 1));
        reqVO.setPeriodEnd(LocalDate.of(2026, 10, 31));

        CustomerStatementPrefetchRespVO respVO = new CustomerStatementPrefetchRespVO();
        respVO.setOpeningBalance(BigDecimal.valueOf(1000));
        respVO.setLines(Collections.emptyList());
        when(customerStatementService.prefetchLines(reqVO)).thenReturn(respVO);

        CommonResult<CustomerStatementPrefetchRespVO> result = controller.prefetchLines(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(customerStatementService).prefetchLines(reqVO);
    }

    @Test
    @DisplayName("createCustomerStatement 接口：委托 Service 并返回主键ID")
    void testCreateCustomerStatement() {
        CustomerStatementCreateReqVO reqVO = new CustomerStatementCreateReqVO();
        reqVO.setCustomerId(101L);
        when(customerStatementService.createCustomerStatement(reqVO)).thenReturn(200L);

        CommonResult<Long> result = controller.createCustomerStatement(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(200L, result.getData());
        verify(customerStatementService).createCustomerStatement(reqVO);
    }

    @Test
    @DisplayName("getCustomerStatement 接口：委托 Service 并返回详情")
    void testGetCustomerStatement() {
        CustomerStatementDetailRespVO detailVO = new CustomerStatementDetailRespVO();
        detailVO.setId(200L);
        detailVO.setStatementNo("CS20260001");
        when(customerStatementService.getCustomerStatement(200L)).thenReturn(detailVO);

        CommonResult<CustomerStatementDetailRespVO> result = controller.getCustomerStatement(200L);

        assertTrue(result.isSuccess());
        assertEquals(detailVO, result.getData());
        verify(customerStatementService).getCustomerStatement(200L);
    }

    @Test
    @DisplayName("getCustomerStatementPage 接口：委托 Service 并返回分页")
    void testGetCustomerStatementPage() {
        CustomerStatementPageReqVO reqVO = new CustomerStatementPageReqVO();
        PageResult<CustomerStatementRespVO> page = PageResult.empty();
        when(customerStatementService.getCustomerStatementPage(reqVO)).thenReturn(page);

        CommonResult<PageResult<CustomerStatementRespVO>> result = controller.getCustomerStatementPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(page, result.getData());
        verify(customerStatementService).getCustomerStatementPage(reqVO);
    }

    @Test
    @DisplayName("updateCustomerStatement 接口：委托 Service 并返回成功")
    void testUpdateCustomerStatement() {
        CustomerStatementUpdateReqVO reqVO = new CustomerStatementUpdateReqVO();
        reqVO.setId(200L);
        reqVO.setRemark("新备注");

        CommonResult<Boolean> result = controller.updateCustomerStatement(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).updateCustomerStatement(reqVO);
    }

    @Test
    @DisplayName("deleteCustomerStatement 接口：委托 Service 并返回成功")
    void testDeleteCustomerStatement() {
        CommonResult<Boolean> result = controller.deleteCustomerStatement(200L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).deleteCustomerStatement(200L);
    }

    @Test
    @DisplayName("addStatementEntry 接口：委托 Service 并返回新增对账单行ID")
    void testAddStatementEntry() {
        CustomerStatementAddEntryReqVO reqVO = new CustomerStatementAddEntryReqVO();
        reqVO.setStatementId(200L);
        reqVO.setLedgerEntryId(555L);
        when(customerStatementService.addStatementEntry(reqVO)).thenReturn(999L);

        CommonResult<Long> result = controller.addStatementEntry(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(999L, result.getData());
        verify(customerStatementService).addStatementEntry(reqVO);
    }

    @Test
    @DisplayName("sendCustomerStatement 接口：委托 Service 执行发送并返回成功")
    void testSendCustomerStatement() {
        CommonResult<Boolean> result = controller.sendCustomerStatement(200L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).sendCustomerStatement(200L);
    }

    @Test
    @DisplayName("voidCustomerStatement 接口：委托 Service 执行作废并返回成功")
    void testVoidCustomerStatement() {
        CommonResult<Boolean> result = controller.voidCustomerStatement(200L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).voidCustomerStatement(200L);
    }

    @Test
    @DisplayName("confirmOnBehalf 接口：委托 Service 执行代客确认并返回成功")
    void testConfirmOnBehalf() {
        CustomerStatementConfirmOnBehalfReqVO reqVO = new CustomerStatementConfirmOnBehalfReqVO();
        reqVO.setId(200L);
        reqVO.setConfirmMethod("微信");
        reqVO.setRemark("客户李总微信确认");

        CommonResult<Boolean> result = controller.confirmOnBehalf(reqVO, null, null, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).confirmOnBehalf(any(CustomerStatementConfirmOnBehalfReqVO.class));
    }

    @Test
    @DisplayName("rejectCustomerStatement 接口：委托 Service 执行驳回并返回成功")
    void testRejectCustomerStatement() {
        CustomerStatementRejectReqVO reqVO = new CustomerStatementRejectReqVO();
        reqVO.setId(200L);
        reqVO.setRejectReason("明细数量有误");

        CommonResult<Boolean> result = controller.rejectCustomerStatement(reqVO, null, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).rejectCustomerStatement(any(CustomerStatementRejectReqVO.class));
    }

    @Test
    @DisplayName("rejectReplyCustomerStatement 接口：委托 Service 执行回复并返回成功")
    void testRejectReplyCustomerStatement() {
        CustomerStatementRejectReplyReqVO reqVO = new CustomerStatementRejectReplyReqVO();
        reqVO.setId(200L);
        reqVO.setReplyRemark("已调整");

        CommonResult<Boolean> result = controller.rejectReplyCustomerStatement(reqVO, null, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).rejectReplyCustomerStatement(any(CustomerStatementRejectReplyReqVO.class));
    }

    @Test
    @DisplayName("getCustomerStatementPrintData 接口：委托 Service 获得打印 VO")
    void testGetCustomerStatementPrintData() {
        CustomerStatementPrintRespVO printVO = new CustomerStatementPrintRespVO();
        printVO.setId(200L);
        printVO.setStatementNo("CS20260001");
        when(customerStatementService.getCustomerStatementPrintData(200L)).thenReturn(printVO);

        CommonResult<CustomerStatementPrintRespVO> result = controller.getCustomerStatementPrintData(200L);
        assertTrue(result.isSuccess());
        assertEquals(printVO, result.getData());
        verify(customerStatementService).getCustomerStatementPrintData(200L);
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("prefetchLines", new Class<?>[]{CustomerStatementPrefetchReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertPermission("createCustomerStatement", new Class<?>[]{CustomerStatementCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:create')");
        assertPermission("getCustomerStatement", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertPermission("getCustomerStatementPage", new Class<?>[]{CustomerStatementPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertPermission("updateCustomerStatement", new Class<?>[]{CustomerStatementUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:update')");
        assertPermission("deleteCustomerStatement", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:delete')");
        assertPermission("addStatementEntry", new Class<?>[]{CustomerStatementAddEntryReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:update')");
        assertPermission("sendCustomerStatement", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:send')");
        assertPermission("voidCustomerStatement", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:void')");
        assertPermission("confirmOnBehalf", new Class<?>[]{CustomerStatementConfirmOnBehalfReqVO.class, Long.class, String.class, String.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:confirm-on-behalf')");
        assertPermission("rejectCustomerStatement", new Class<?>[]{CustomerStatementRejectReqVO.class, Long.class, String.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:update')");
        assertPermission("rejectReplyCustomerStatement", new Class<?>[]{CustomerStatementRejectReplyReqVO.class, Long.class, String.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:update')");
        assertPermission("getCustomerStatementPrintData", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:print')");
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = CustomerStatementController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}

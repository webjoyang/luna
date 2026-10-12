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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerStatementControllerTest {

    @Mock
    private CustomerStatementService customerStatementService;

    @InjectMocks
    private CustomerStatementController customerStatementController;

    @Test
    @DisplayName("分页查询客户对账单")
    void testGetStatementPage() {
        CustomerStatementPageReqVO reqVO = new CustomerStatementPageReqVO();
        CustomerStatementRespVO respVO = CustomerStatementRespVO.builder().id(100L).statementNo("CS20260001").build();
        when(customerStatementService.getStatementPage(any())).thenReturn(new PageResult<>(List.of(respVO), 1L));

        CommonResult<PageResult<CustomerStatementRespVO>> result = customerStatementController.getStatementPage(reqVO);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals("CS20260001", result.getData().getList().get(0).getStatementNo());
    }

    @Test
    @DisplayName("获取客户对账单详情")
    void testGetStatement() {
        CustomerStatementDetailRespVO detail = new CustomerStatementDetailRespVO();
        detail.setId(100L);
        detail.setStatementNo("CS20260001");
        detail.setItems(List.of(CustomerStatementItemRespVO.builder().id(1L).lineNo(1).build()));
        when(customerStatementService.getStatement(100L)).thenReturn(detail);

        CommonResult<CustomerStatementDetailRespVO> result = customerStatementController.getStatement(100L);
        assertEquals(0, result.getCode());
        assertEquals(100L, result.getData().getId());
        assertEquals(1, result.getData().getItems().size());
    }

    @Test
    @DisplayName("取数预览候选分录")
    void testPrefetchLines() {
        CustomerStatementPrefetchReqVO req = CustomerStatementPrefetchReqVO.builder()
                .customerId(1001L).currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .build();
        CustomerStatementPrefetchRespVO resp = CustomerStatementPrefetchRespVO.builder()
                .customerId(1001L).openingBalance(new BigDecimal("5000.00")).build();
        when(customerStatementService.prefetchLines(any())).thenReturn(resp);

        CommonResult<CustomerStatementPrefetchRespVO> result = customerStatementController.prefetchLines(req);
        assertEquals(0, result.getCode());
        assertEquals(1001L, result.getData().getCustomerId());
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getData().getOpeningBalance()));
    }

    @Test
    @DisplayName("创建草稿对账单")
    void testCreateStatement() {
        CustomerStatementCreateReqVO req = CustomerStatementCreateReqVO.builder()
                .customerId(1001L).currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .build();
        when(customerStatementService.createStatement(any())).thenReturn(888L);

        CommonResult<Long> result = customerStatementController.createStatement(req);
        assertEquals(0, result.getCode());
        assertEquals(888L, result.getData());
    }

    @Test
    @DisplayName("更新草稿对账单")
    void testUpdateStatement() {
        CustomerStatementUpdateReqVO req = CustomerStatementUpdateReqVO.builder().id(888L).remark("修改备注").build();
        doNothing().when(customerStatementService).updateStatement(any());

        CommonResult<Boolean> result = customerStatementController.updateStatement(req);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerStatementService).updateStatement(req);
    }

    @Test
    @DisplayName("删除草稿对账单")
    void testDeleteStatement() {
        doNothing().when(customerStatementService).deleteStatement(888L);

        CommonResult<Boolean> result = customerStatementController.deleteStatement(888L);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerStatementService).deleteStatement(888L);
    }

    @Test
    @DisplayName("草稿手工追加分录")
    void testAddEntry() {
        CustomerStatementAddEntryReqVO req = CustomerStatementAddEntryReqVO.builder().statementId(888L).ledgerEntryId(501L).build();
        doNothing().when(customerStatementService).addEntry(any());

        CommonResult<Boolean> result = customerStatementController.addEntry(req);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerStatementService).addEntry(req);
    }

}

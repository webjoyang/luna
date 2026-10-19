package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event.SalesReturnPostedEvent;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event.SalesReturnPostedItemDTO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 销售退货过账事件财务记账监听器单元测试（SalesReturnLedgerListenerTest）
 * 覆盖 Issue: .scratch/sales-return/issues/06-post-ar-reversal.md
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class SalesReturnLedgerListenerTest {

    @InjectMocks
    private SalesReturnLedgerListener listener;

    @Mock
    private LedgerService ledgerService;

    @Test
    @DisplayName("onSalesReturnPosted: 正常过账，按原发货入账汇率逐行调 recordArReturnReversal 冲减应收")
    void testOnSalesReturnPosted_success_callsRecordArReturnReversal() {
        SalesReturnPostedItemDTO item1 = SalesReturnPostedItemDTO.builder()
                .returnItemId(201L)
                .shipmentItemId(1001L)
                .returnQty(new BigDecimal("50.00"))
                .amount(new BigDecimal("1200.00"))
                .build();

        SalesReturnPostedItemDTO item2 = SalesReturnPostedItemDTO.builder()
                .returnItemId(202L)
                .shipmentItemId(1002L)
                .returnQty(new BigDecimal("30.00"))
                .amount(new BigDecimal("900.00"))
                .build();

        SalesReturnPostedEvent event = SalesReturnPostedEvent.builder()
                .returnId(10L)
                .returnNo("RT-20261019-001")
                .shipmentId(88L)
                .shipmentNo("SH-20261016-001")
                .customerId(501L)
                .currency("USD")
                .returnDate(LocalDate.of(2026, 10, 19))
                .items(Arrays.asList(item1, item2))
                .build();

        // 模拟原发货 AR_INC 分录
        LedgerEntryDO origEntry1 = new LedgerEntryDO();
        origEntry1.setId(101L);
        origEntry1.setCategory("AR_INC");
        origEntry1.setRefType("SHIPMENT_ITEM");
        origEntry1.setRefId(88L);
        origEntry1.setRefItemId(1001L);
        origEntry1.setExchangeRate(new BigDecimal("7.15000000"));

        LedgerEntryDO origEntry2 = new LedgerEntryDO();
        origEntry2.setId(102L);
        origEntry2.setCategory("AR_INC");
        origEntry2.setRefType("SHIPMENT_ITEM");
        origEntry2.setRefId(88L);
        origEntry2.setRefItemId(1002L);
        origEntry2.setExchangeRate(new BigDecimal("7.15000000"));

        when(ledgerService.getEntriesByRef("SHIPMENT_ITEM", 88L))
                .thenReturn(Arrays.asList(origEntry1, origEntry2));

        // Act
        listener.onSalesReturnPosted(event);

        // Assert
        verify(ledgerService).recordArReturnReversal(
                eq(501L), eq(1001L), eq(new BigDecimal("50.00")),
                eq(new BigDecimal("1200.00")), eq("USD"),
                eq(new BigDecimal("7.15000000")), eq(10L));

        verify(ledgerService).recordArReturnReversal(
                eq(501L), eq(1002L), eq(new BigDecimal("30.00")),
                eq(new BigDecimal("900.00")), eq("USD"),
                eq(new BigDecimal("7.15000000")), eq(10L));
    }

    @Test
    @DisplayName("onSalesReturnPosted: 原发货入账分录缺失或汇率缺失抛出 014 异常触发事务回滚")
    void testOnSalesReturnPosted_missingRate_throws014Exception() {
        SalesReturnPostedItemDTO item = SalesReturnPostedItemDTO.builder()
                .returnItemId(201L)
                .shipmentItemId(1001L)
                .returnQty(new BigDecimal("50.00"))
                .amount(new BigDecimal("1200.00"))
                .build();

        SalesReturnPostedEvent event = SalesReturnPostedEvent.builder()
                .returnId(10L)
                .returnNo("RT-20261019-001")
                .shipmentId(88L)
                .customerId(501L)
                .currency("USD")
                .items(Collections.singletonList(item))
                .build();

        // 模拟未查询到任何原发货分录
        when(ledgerService.getEntriesByRef("SHIPMENT_ITEM", 88L))
                .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> listener.onSalesReturnPosted(event));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING.getCode(), ex.getCode());
        verify(ledgerService, never()).recordArReturnReversal(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("onSalesReturnPosted: 原发货分录存在但 exchangeRate 为 null 抛出 014 异常")
    void testOnSalesReturnPosted_nullRate_throws014Exception() {
        SalesReturnPostedItemDTO item = SalesReturnPostedItemDTO.builder()
                .returnItemId(201L)
                .shipmentItemId(1001L)
                .returnQty(new BigDecimal("50.00"))
                .amount(new BigDecimal("1200.00"))
                .build();

        SalesReturnPostedEvent event = SalesReturnPostedEvent.builder()
                .returnId(10L)
                .returnNo("RT-20261019-001")
                .shipmentId(88L)
                .customerId(501L)
                .currency("USD")
                .items(Collections.singletonList(item))
                .build();

        LedgerEntryDO origEntry = new LedgerEntryDO();
        origEntry.setId(101L);
        origEntry.setCategory("AR_INC");
        origEntry.setRefItemId(1001L);
        origEntry.setExchangeRate(null);

        when(ledgerService.getEntriesByRef("SHIPMENT_ITEM", 88L))
                .thenReturn(Collections.singletonList(origEntry));

        ServiceException ex = assertThrows(ServiceException.class, () -> listener.onSalesReturnPosted(event));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING.getCode(), ex.getCode());
        verify(ledgerService, never()).recordArReturnReversal(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("onSalesReturnPosted: 明细行缺少 shipmentItemId 血缘抛出 014 异常")
    void testOnSalesReturnPosted_itemMissingShipmentItemId_throws014Exception() {
        SalesReturnPostedItemDTO item = SalesReturnPostedItemDTO.builder()
                .returnItemId(201L)
                .shipmentItemId(null)
                .returnQty(new BigDecimal("50.00"))
                .amount(new BigDecimal("1200.00"))
                .build();

        SalesReturnPostedEvent event = SalesReturnPostedEvent.builder()
                .returnId(10L)
                .returnNo("RT-20261019-001")
                .shipmentId(88L)
                .customerId(501L)
                .currency("USD")
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> listener.onSalesReturnPosted(event));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING.getCode(), ex.getCode());
        verify(ledgerService, never()).recordArReturnReversal(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("onSalesReturnPosted: 空事件或空明细安全跳过")
    void testOnSalesReturnPosted_emptyEvent_skips() {
        listener.onSalesReturnPosted(null);
        listener.onSalesReturnPosted(new SalesReturnPostedEvent());
        verify(ledgerService, never()).getEntriesByRef(any(), any());
        verify(ledgerService, never()).recordArReturnReversal(any(), any(), any(), any(), any(), any(), any());
    }

}

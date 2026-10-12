package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentAuditedEvent;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentCancelledEvent;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShipmentFinanceListenerTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ShipmentAuditFinanceListener auditListener;

    @InjectMocks
    private ShipmentCancelFinanceListener cancelListener;

    @Test
    @DisplayName("ShipmentAuditedEvent：明细行+运费行折算记账")
    void testOnShipmentAudited_WritesItemAndFreightEntries() {
        LocalDate shipDate = LocalDate.of(2026, 10, 12);
        when(exchangeRateService.getRate("USD", shipDate)).thenReturn(new BigDecimal("7.20000000"));

        ShipmentAuditedEvent.Item item = ShipmentAuditedEvent.Item.builder()
                .shipmentItemId(101L)
                .productName("全棉斜纹面料")
                .quantity(new BigDecimal("100"))
                .unit("M")
                .unitPrice(new BigDecimal("10.00"))
                .amount(new BigDecimal("1000.00"))
                .build();

        ShipmentAuditedEvent event = ShipmentAuditedEvent.builder()
                .shipmentId(10L)
                .shipmentNo("SH20261012001")
                .customerId(200L)
                .currency("USD")
                .shipmentDate(shipDate)
                .freightFee(new BigDecimal("50.00"))
                .items(Collections.singletonList(item))
                .build();

        auditListener.onShipmentAudited(event);

        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(2)).append(captor.capture());
        List<LedgerEntryDO> entries = captor.getAllValues();

        // 1. 明细行
        LedgerEntryDO itemEntry = entries.get(0);
        assertEquals(FinanceLedgerCategoryEnum.AR_INC.getValue(), itemEntry.getCategory());
        assertEquals(FinanceLedgerDirectionEnum.DEBIT.getValue(), itemEntry.getDirection());
        assertEquals(FinanceLedgerRefTypeEnum.SHIPMENT_ITEM.getValue(), itemEntry.getRefType());
        assertEquals(10L, itemEntry.getRefId());
        assertEquals(101L, itemEntry.getRefItemId());
        assertEquals(new BigDecimal("1000.00"), itemEntry.getOriginalAmount());
        assertEquals(new BigDecimal("7200.00"), itemEntry.getBaseAmount());

        // 2. 运费行
        LedgerEntryDO freightEntry = entries.get(1);
        assertEquals(FinanceLedgerCategoryEnum.AR_INC.getValue(), freightEntry.getCategory());
        assertEquals(FinanceLedgerDirectionEnum.DEBIT.getValue(), freightEntry.getDirection());
        assertEquals(FinanceLedgerRefTypeEnum.SHIPMENT_FREIGHT.getValue(), freightEntry.getRefType());
        assertEquals(10L, freightEntry.getRefId());
        assertEquals(new BigDecimal("50.00"), freightEntry.getOriginalAmount());
        assertEquals(new BigDecimal("360.00"), freightEntry.getBaseAmount());
    }

    @Test
    @DisplayName("ShipmentCancelledEvent：追加 AR_INC_REVERSAL 分录回指原分录")
    void testOnShipmentCancelled_WritesReversalEntries() {
        LedgerEntryDO origEntry = LedgerEntryDO.builder()
                .id(1001L)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .partyType("CUSTOMER")
                .partyId(200L)
                .currency("USD")
                .exchangeRate(new BigDecimal("7.20000000"))
                .originalAmount(new BigDecimal("1000.00"))
                .baseAmount(new BigDecimal("7200.00"))
                .refType(FinanceLedgerRefTypeEnum.SHIPMENT_ITEM.getValue())
                .refId(10L)
                .refItemId(101L)
                .build();

        when(ledgerService.getEntriesByRefId(10L)).thenReturn(Collections.singletonList(origEntry));
        when(ledgerService.getEntriesByReversalOfId(1001L)).thenReturn(Collections.emptyList());

        ShipmentCancelledEvent event = ShipmentCancelledEvent.builder()
                .shipmentId(10L)
                .shipmentNo("SH20261012001")
                .customerId(200L)
                .build();

        cancelListener.onShipmentCancelled(event);

        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(1)).append(captor.capture());
        LedgerEntryDO reversal = captor.getValue();

        assertEquals(FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue(), reversal.getCategory());
        assertEquals(FinanceLedgerDirectionEnum.CREDIT.getValue(), reversal.getDirection());
        assertEquals(1001L, reversal.getReversalOfId());
        assertEquals(new BigDecimal("1000.00"), reversal.getOriginalAmount());
        assertEquals(new BigDecimal("7200.00"), reversal.getBaseAmount());
    }
}

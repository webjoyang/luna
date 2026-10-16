package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentAuditedEvent;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentCancelledEvent;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentItemSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 发货事件财务记账监听器单元测试（接缝 5）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ShipmentFinanceEventListenerTest {

    @InjectMocks
    private ShipmentFinanceEventListener listener;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @Test
    @DisplayName("onShipmentAudited: 发货审核成功，逐明细行写入 AR_INC 分录 + 运费写入一条 AR_INC 分录")
    void testOnShipmentAudited_success_writesItemAndFreightEntries() {
        LocalDate shipDate = LocalDate.of(2026, 10, 16);
        ShipmentItemSnapshotDTO item1 = ShipmentItemSnapshotDTO.builder()
                .shipmentItemId(1001L)
                .productId(201L)
                .productCode("PRD-01")
                .productName("纯棉面料")
                .quantity(new BigDecimal("100.00"))
                .unit("M")
                .unitPrice(new BigDecimal("25.00"))
                .amount(new BigDecimal("2500.00"))
                .remark("明细1")
                .build();

        ShipmentItemSnapshotDTO item2 = ShipmentItemSnapshotDTO.builder()
                .shipmentItemId(1002L)
                .productId(202L)
                .productCode("PRD-02")
                .productName("涤纶面料")
                .quantity(new BigDecimal("200.00"))
                .unit("M")
                .unitPrice(new BigDecimal("15.00"))
                .amount(new BigDecimal("3000.00"))
                .remark("明细2")
                .build();

        ShipmentAuditedEvent event = ShipmentAuditedEvent.builder()
                .shipmentId(88L)
                .shipmentNo("SH-20261016-001")
                .customerId(501L)
                .customerName("外贸客户A")
                .currency("USD")
                .shipmentDate(shipDate)
                .freightFee(new BigDecimal("200.00"))
                .items(List.of(item1, item2))
                .build();

        when(exchangeRateService.getRate("USD", shipDate)).thenReturn(new BigDecimal("7.10000000"));

        // Act
        listener.onShipmentAudited(event);

        // Assert
        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(3)).append(captor.capture());

        List<LedgerEntryDO> entries = captor.getAllValues();
        assertEquals(3, entries.size());

        // 明细行 1
        LedgerEntryDO entry1 = entries.get(0);
        assertEquals(shipDate, entry1.getBizDate());
        assertEquals("CUSTOMER", entry1.getPartyType());
        assertEquals(501L, entry1.getPartyId());
        assertNull(entry1.getAccountId());
        assertEquals("AR_INC", entry1.getCategory());
        assertEquals("DEBIT", entry1.getDirection());
        assertEquals("USD", entry1.getCurrency());
        assertEquals(new BigDecimal("7.10000000"), entry1.getExchangeRate());
        assertEquals(new BigDecimal("2500.00"), entry1.getOriginalAmount());
        assertEquals(new BigDecimal("17750.00"), entry1.getBaseAmount()); // 2500 * 7.1
        assertEquals("SHIPMENT_ITEM", entry1.getRefType());
        assertEquals(88L, entry1.getRefId());
        assertEquals(1001L, entry1.getRefItemId());
        assertNull(entry1.getReversalOfId());

        // 明细行 2
        LedgerEntryDO entry2 = entries.get(1);
        assertEquals("SHIPMENT_ITEM", entry2.getRefType());
        assertEquals(1002L, entry2.getRefItemId());
        assertEquals(new BigDecimal("3000.00"), entry2.getOriginalAmount());
        assertEquals(new BigDecimal("21300.00"), entry2.getBaseAmount()); // 3000 * 7.1

        // 运费行
        LedgerEntryDO freightEntry = entries.get(2);
        assertEquals("SHIPMENT_FREIGHT", freightEntry.getRefType());
        assertEquals(88L, freightEntry.getRefId());
        assertNull(freightEntry.getRefItemId());
        assertEquals("AR_INC", freightEntry.getCategory());
        assertEquals("DEBIT", freightEntry.getDirection());
        assertEquals(new BigDecimal("200.00"), freightEntry.getOriginalAmount());
        assertEquals(new BigDecimal("1420.00"), freightEntry.getBaseAmount()); // 200 * 7.1
    }

    @Test
    @DisplayName("onShipmentAudited: 运费为0或空时仅写入货物明细分录，不写运费分录")
    void testOnShipmentAudited_zeroFreight_writesOnlyItemEntries() {
        LocalDate shipDate = LocalDate.of(2026, 10, 16);
        ShipmentItemSnapshotDTO item1 = ShipmentItemSnapshotDTO.builder()
                .shipmentItemId(1001L)
                .productId(201L)
                .amount(new BigDecimal("1000.00"))
                .build();

        ShipmentAuditedEvent event = ShipmentAuditedEvent.builder()
                .shipmentId(88L)
                .shipmentNo("SH-20261016-001")
                .customerId(501L)
                .currency("RMB")
                .shipmentDate(shipDate)
                .freightFee(BigDecimal.ZERO)
                .items(List.of(item1))
                .build();

        when(exchangeRateService.getRate("RMB", shipDate)).thenReturn(BigDecimal.ONE);

        listener.onShipmentAudited(event);

        verify(ledgerService, times(1)).append(any(LedgerEntryDO.class));
    }

    @Test
    @DisplayName("onShipmentAudited: 缺少汇率抛出异常，触发事务回滚")
    void testOnShipmentAudited_rateNotFound_throwsException() {
        LocalDate shipDate = LocalDate.of(2026, 10, 16);
        ShipmentAuditedEvent event = ShipmentAuditedEvent.builder()
                .shipmentId(88L)
                .currency("EUR")
                .shipmentDate(shipDate)
                .items(List.of(ShipmentItemSnapshotDTO.builder().amount(BigDecimal.TEN).build()))
                .build();

        when(exchangeRateService.getRate("EUR", shipDate))
                .thenThrow(new ServiceException(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(),
                        FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getMsg()));

        ServiceException ex = assertThrows(ServiceException.class, () -> listener.onShipmentAudited(event));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
        verify(ledgerService, never()).append(any(LedgerEntryDO.class));
    }

    @Test
    @DisplayName("onShipmentCancelled: 发货红冲成功，反查原分录并写入 AR_INC_REVERSAL 分录（携带 reversal_of_id 回指）")
    void testOnShipmentCancelled_success_writesReversalEntriesWithReversalOfId() {
        ShipmentCancelledEvent event = ShipmentCancelledEvent.builder()
                .shipmentId(88L)
                .shipmentNo("SH-20261016-001")
                .customerId(501L)
                .currency("RMB")
                .cancelDate(LocalDate.of(2026, 10, 17))
                .build();

        // 模拟原分录：明细分录 1 条（id=101）、运费分录 1 条（id=102）
        LedgerEntryDO origItem = new LedgerEntryDO();
        origItem.setId(101L);
        origItem.setBizDate(LocalDate.of(2026, 10, 16));
        origItem.setPartyType("CUSTOMER");
        origItem.setPartyId(501L);
        origItem.setCategory("AR_INC");
        origItem.setDirection("DEBIT");
        origItem.setCurrency("RMB");
        origItem.setExchangeRate(BigDecimal.ONE);
        origItem.setOriginalAmount(new BigDecimal("5000.00"));
        origItem.setBaseAmount(new BigDecimal("5000.00"));
        origItem.setRefType("SHIPMENT_ITEM");
        origItem.setRefId(88L);
        origItem.setRefItemId(1001L);
        origItem.setRemark("纯棉面料");

        LedgerEntryDO origFreight = new LedgerEntryDO();
        origFreight.setId(102L);
        origFreight.setBizDate(LocalDate.of(2026, 10, 16));
        origFreight.setPartyType("CUSTOMER");
        origFreight.setPartyId(501L);
        origFreight.setCategory("AR_INC");
        origFreight.setDirection("DEBIT");
        origFreight.setCurrency("RMB");
        origFreight.setExchangeRate(BigDecimal.ONE);
        origFreight.setOriginalAmount(new BigDecimal("150.00"));
        origFreight.setBaseAmount(new BigDecimal("150.00"));
        origFreight.setRefType("SHIPMENT_FREIGHT");
        origFreight.setRefId(88L);
        origFreight.setRemark("发货运费");

        when(ledgerService.getEntriesByRef("SHIPMENT_ITEM", 88L)).thenReturn(List.of(origItem));
        when(ledgerService.getEntriesByRef("SHIPMENT_FREIGHT", 88L)).thenReturn(List.of(origFreight));
        when(ledgerService.getReversalEntries(anyLong())).thenReturn(Collections.emptyList());

        // Act
        listener.onShipmentCancelled(event);

        // Assert
        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(2)).append(captor.capture());

        List<LedgerEntryDO> reversals = captor.getAllValues();
        assertEquals(2, reversals.size());

        // 红冲明细分录
        LedgerEntryDO revItem = reversals.get(0);
        assertEquals("AR_INC_REVERSAL", revItem.getCategory());
        assertEquals("CREDIT", revItem.getDirection());
        assertEquals(new BigDecimal("5000.00"), revItem.getOriginalAmount());
        assertEquals(new BigDecimal("5000.00"), revItem.getBaseAmount());
        assertEquals("SHIPMENT_ITEM", revItem.getRefType());
        assertEquals(88L, revItem.getRefId());
        assertEquals(1001L, revItem.getRefItemId());
        assertEquals(101L, revItem.getReversalOfId()); // 关键断言：回指原分录ID

        // 红冲运费分录
        LedgerEntryDO revFreight = reversals.get(1);
        assertEquals("AR_INC_REVERSAL", revFreight.getCategory());
        assertEquals("CREDIT", revFreight.getDirection());
        assertEquals(new BigDecimal("150.00"), revFreight.getOriginalAmount());
        assertEquals(new BigDecimal("150.00"), revFreight.getBaseAmount());
        assertEquals("SHIPMENT_FREIGHT", revFreight.getRefType());
        assertEquals(88L, revFreight.getRefId());
        assertEquals(102L, revFreight.getReversalOfId()); // 关键断言：回指原运费分录ID
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

/**
 * 经营分录账 Service 单元测试（接缝 1）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class LedgerServiceImplTest {

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @Test
    @DisplayName("append: 正常追加分录并持久化，返回分录ID")
    void testAppend_success() {
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setBizDate(LocalDate.of(2026, 10, 16));
        entry.setPartyType("CUSTOMER");
        entry.setPartyId(1001L);
        entry.setCategory("AR_INC");
        entry.setDirection("DEBIT");
        entry.setCurrency("RMB");
        entry.setExchangeRate(new BigDecimal("1.00000000"));
        entry.setOriginalAmount(new BigDecimal("5000.00"));
        entry.setBaseAmount(new BigDecimal("5000.00"));
        entry.setRefType("SHIPMENT_ITEM");
        entry.setRefId(2001L);
        entry.setRefItemId(3001L);

        doAnswer(invocation -> {
            LedgerEntryDO arg = invocation.getArgument(0);
            arg.setId(888L);
            return 1;
        }).when(ledgerEntryMapper).insert(any(LedgerEntryDO.class));

        Long id = ledgerService.append(entry);

        assertNotNull(id);
        assertEquals(888L, id);
        verify(ledgerEntryMapper, times(1)).insert(entry);
    }

    @Test
    @DisplayName("update: 应用层禁止修改分录，抛出 1_030_020_000 LEDGER_IMMUTABLE")
    void testUpdate_throwsLedgerImmutable() {
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(888L);

        ServiceException ex = assertThrows(ServiceException.class, () -> ledgerService.updateEntry(entry));
        assertEquals(FinanceErrorCodeConstants.LEDGER_IMMUTABLE.getCode(), ex.getCode());
        verify(ledgerEntryMapper, never()).updateById(any(LedgerEntryDO.class));
    }

    @Test
    @DisplayName("delete: 应用层禁止删除分录，抛出 1_030_020_000 LEDGER_IMMUTABLE")
    void testDelete_throwsLedgerImmutable() {
        ServiceException ex = assertThrows(ServiceException.class, () -> ledgerService.deleteEntry(888L));
        assertEquals(FinanceErrorCodeConstants.LEDGER_IMMUTABLE.getCode(), ex.getCode());
        verify(ledgerEntryMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("getEntriesByRef: 根据业务回指类型与主单ID检索分录列表")
    void testGetEntriesByRef_success() {
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(888L);
        entry.setRefType("SHIPMENT_ITEM");
        entry.setRefId(2001L);

        when(ledgerEntryMapper.selectListByRef("SHIPMENT_ITEM", 2001L)).thenReturn(List.of(entry));

        List<LedgerEntryDO> result = ledgerService.getEntriesByRef("SHIPMENT_ITEM", 2001L);
        assertEquals(1, result.size());
        assertEquals(888L, result.get(0).getId());
    }

    @Test
    @DisplayName("recordArReturnReversal: 销售退货冲减直接追加反向 CREDIT 分录，折合本位币正确")
    void testRecordArReturnReversal_success() {
        Long customerId = 1001L;
        Long shipmentItemId = 2001L;
        BigDecimal returnQty = new BigDecimal("50.00");
        BigDecimal origAmount = new BigDecimal("1200.00");
        String currency = "USD";
        BigDecimal exchangeRate = new BigDecimal("7.15000000");
        Long returnOrderId = 3001L;

        doAnswer(invocation -> {
            LedgerEntryDO arg = invocation.getArgument(0);
            arg.setId(999L);
            return 1;
        }).when(ledgerEntryMapper).insert(any(LedgerEntryDO.class));

        Long entryId = ledgerService.recordArReturnReversal(customerId, shipmentItemId, returnQty,
                origAmount, currency, exchangeRate, returnOrderId);

        assertNotNull(entryId);
        assertEquals(999L, entryId);

        org.mockito.ArgumentCaptor<LedgerEntryDO> captor = org.mockito.ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerEntryMapper).insert(captor.capture());
        LedgerEntryDO saved = captor.getValue();
        assertEquals("CUSTOMER", saved.getPartyType());
        assertEquals(customerId, saved.getPartyId());
        assertEquals("AR_RETURN_REVERSAL", saved.getCategory());
        assertEquals("CREDIT", saved.getDirection());
        assertEquals("USD", saved.getCurrency());
        assertEquals(exchangeRate, saved.getExchangeRate());
        assertEquals(origAmount, saved.getOriginalAmount());
        assertEquals(new BigDecimal("8580.00"), saved.getBaseAmount()); // 1200 * 7.15 = 8580.00
        assertEquals("SALES_RETURN", saved.getRefType());
        assertEquals(returnOrderId, saved.getRefId());
        assertEquals(shipmentItemId, saved.getRefItemId());
    }

}


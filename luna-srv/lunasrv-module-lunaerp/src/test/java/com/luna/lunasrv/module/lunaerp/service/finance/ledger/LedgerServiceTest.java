package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.LEDGER_IMMUTABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link LedgerServiceImpl} 单元测试
 *
 * 覆盖 Issue 02:
 * 1. append 追加分录（唯一写入通道、自动折算本位币、默认汇率与日期）
 * 2. 禁改禁删守卫：update/delete 端点抛出 1_030_020_000 LEDGER_IMMUTABLE
 * 3. recordArReturnReversal 销售退货冲减分录契约（Issue 13 接缝）
 * 4. 多维度分录查询方法（主体、源单、账户流水等）
 */
class LedgerServiceTest {

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("append：正常写入分录，自动计算本位币金额，返回自增ID")
    void testAppend_Success_CalculatesBaseAmount() {
        LedgerEntryDO entry = LedgerEntryDO.builder()
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(101L)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency("USD")
                .exchangeRate(new BigDecimal("7.20000000"))
                .originalAmount(new BigDecimal("100.00"))
                .refType(FinanceLedgerRefTypeEnum.SHIPMENT_ITEM.getValue())
                .refId(201L)
                .refItemId(301L)
                .build();

        when(ledgerEntryMapper.insert(any(LedgerEntryDO.class))).thenAnswer(invocation -> {
            LedgerEntryDO arg = invocation.getArgument(0);
            arg.setId(1001L);
            return 1;
        });

        Long id = ledgerService.append(entry);

        assertNotNull(id);
        assertEquals(1001L, id);
        assertEquals(new BigDecimal("720.00"), entry.getBaseAmount());
        assertNotNull(entry.getBizDate());
        verify(ledgerEntryMapper, times(1)).insert(entry);
    }

    @Test
    @DisplayName("append：汇率为空时默认设为 1，本位币等于原币")
    void testAppend_DefaultExchangeRateToOne() {
        LedgerEntryDO entry = LedgerEntryDO.builder()
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(101L)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency("CNY")
                .originalAmount(new BigDecimal("500.50"))
                .refType(FinanceLedgerRefTypeEnum.SHIPMENT_ITEM.getValue())
                .refId(202L)
                .build();

        when(ledgerEntryMapper.insert(any(LedgerEntryDO.class))).thenAnswer(invocation -> {
            LedgerEntryDO arg = invocation.getArgument(0);
            arg.setId(1002L);
            return 1;
        });

        Long id = ledgerService.append(entry);

        assertEquals(1002L, id);
        assertEquals(new BigDecimal("500.50"), entry.getBaseAmount());
        assertEquals(new BigDecimal("1.00000000"), entry.getExchangeRate());
    }

    @Test
    @DisplayName("守卫：调用 update 抛出 1_030_020_000 LEDGER_IMMUTABLE")
    void testUpdate_ThrowsLedgerImmutable() {
        LedgerEntryDO entry = LedgerEntryDO.builder().id(1001L).remark("试图修改").build();
        ServiceException ex = assertThrows(ServiceException.class, () -> ledgerService.update(entry));
        assertEquals(LEDGER_IMMUTABLE.getCode(), ex.getCode());
        verify(ledgerEntryMapper, never()).updateById(any(LedgerEntryDO.class));
    }

    @Test
    @DisplayName("守卫：调用 delete 抛出 1_030_020_000 LEDGER_IMMUTABLE")
    void testDelete_ThrowsLedgerImmutable() {
        ServiceException ex = assertThrows(ServiceException.class, () -> ledgerService.delete(1001L));
        assertEquals(LEDGER_IMMUTABLE.getCode(), ex.getCode());
        verify(ledgerEntryMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("recordArReturnReversal：正确写入 AR_RETURN_REVERSAL 退货冲减贷方分录")
    void testRecordArReturnReversal_Success() {
        when(ledgerEntryMapper.insert(any(LedgerEntryDO.class))).thenAnswer(invocation -> {
            LedgerEntryDO arg = invocation.getArgument(0);
            arg.setId(2001L);
            return 1;
        });

        Long entryId = ledgerService.recordArReturnReversal(
                88L, 301L, new BigDecimal("10.00"), new BigDecimal("200.00"),
                "USD", new BigDecimal("7.25000000"), 501L
        );

        assertEquals(2001L, entryId);
        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerEntryMapper).insert(captor.capture());

        LedgerEntryDO saved = captor.getValue();
        assertEquals(FinancePartyTypeEnum.CUSTOMER.getValue(), saved.getPartyType());
        assertEquals(88L, saved.getPartyId());
        assertEquals(FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue(), saved.getCategory());
        assertEquals(FinanceLedgerDirectionEnum.CREDIT.getValue(), saved.getDirection());
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("7.25000000"), saved.getExchangeRate());
        assertEquals(new BigDecimal("200.00"), saved.getOriginalAmount());
        assertEquals(new BigDecimal("1450.00"), saved.getBaseAmount());
        assertEquals(FinanceLedgerRefTypeEnum.RETURN_ORDER.getValue(), saved.getRefType());
        assertEquals(501L, saved.getRefId());
        assertEquals(301L, saved.getRefItemId());
    }

    @Test
    @DisplayName("查询方法路由覆盖")
    void testQueryMethods() {
        when(ledgerEntryMapper.selectListByParty("CUSTOMER", 1L, LocalDate.now(), LocalDate.now()))
                .thenReturn(List.of(new LedgerEntryDO()));
        assertEquals(1, ledgerService.getEntriesByParty("CUSTOMER", 1L, LocalDate.now(), LocalDate.now()).size());

        when(ledgerEntryMapper.selectListByRef("SHIPMENT", 10L)).thenReturn(List.of(new LedgerEntryDO()));
        assertEquals(1, ledgerService.getEntriesByRef("SHIPMENT", 10L).size());

        when(ledgerEntryMapper.selectListByRefId(10L)).thenReturn(List.of(new LedgerEntryDO()));
        assertEquals(1, ledgerService.getEntriesByRefId(10L).size());

        when(ledgerEntryMapper.selectById(100L)).thenReturn(LedgerEntryDO.builder().id(100L).build());
        assertEquals(100L, ledgerService.getEntryById(100L).getId());

        when(ledgerEntryMapper.selectListByReversalOfId(100L)).thenReturn(List.of(new LedgerEntryDO()));
        assertEquals(1, ledgerService.getEntriesByReversalOfId(100L).size());

        when(ledgerEntryMapper.selectListByAccount(5L, null, null)).thenReturn(List.of(new LedgerEntryDO()));
        assertEquals(1, ledgerService.getEntriesByAccount(5L, null, null).size());

        when(ledgerEntryMapper.selectCountByAccountId(5L)).thenReturn(3L);
        assertEquals(3L, ledgerService.countEntriesByAccountId(5L));
        assertEquals(0L, ledgerService.countEntriesByAccountId(null));
    }

}

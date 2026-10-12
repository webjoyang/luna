package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerCustomerBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
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

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private ExchangeRateService exchangeRateService;

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

    @Test
    @DisplayName("getLedgerPage：分页查询经营分录账并回填客户与账户名称")
    void testGetLedgerPage_Success() {
        LedgerPageReqVO reqVO = new LedgerPageReqVO();
        reqVO.setPartyType("CUSTOMER");
        reqVO.setPartyId(101L);

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .id(1L)
                .bizDate(LocalDate.now())
                .partyType("CUSTOMER")
                .partyId(101L)
                .accountId(5L)
                .category("AR_INC")
                .direction("DEBIT")
                .currency("USD")
                .originalAmount(new BigDecimal("100.00"))
                .baseAmount(new BigDecimal("720.00"))
                .build();

        when(ledgerEntryMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(entry), 1L));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("测试客户");
        when(customerMapper.selectBatchIds(any())).thenReturn(List.of(customer));

        AccountDO account = AccountDO.builder().id(5L).accountName("美金基本户").build();
        when(accountMapper.selectBatchIds(any())).thenReturn(List.of(account));

        PageResult<LedgerRespVO> pageResult = ledgerService.getLedgerPage(reqVO);

        assertNotNull(pageResult);
        assertEquals(1L, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        LedgerRespVO resp = pageResult.getList().get(0);
        assertEquals("测试客户", resp.getPartyName());
        assertEquals("美金基本户", resp.getAccountName());
    }

    @Test
    @DisplayName("getCustomerBalanceList：客户维度余额汇总与汇率差异计算")
    void testGetCustomerBalanceList_WithExchangeDiff() {
        LedgerEntryDO debit = LedgerEntryDO.builder()
                .id(1L)
                .partyType("CUSTOMER")
                .partyId(101L)
                .currency("USD")
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction("DEBIT")
                .originalAmount(new BigDecimal("3000.00"))
                .baseAmount(new BigDecimal("21600.00")) // rate 7.20
                .build();

        LedgerEntryDO credit = LedgerEntryDO.builder()
                .id(2L)
                .partyType("CUSTOMER")
                .partyId(101L)
                .currency("USD")
                .category(FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue())
                .direction("CREDIT")
                .originalAmount(new BigDecimal("1000.00"))
                .baseAmount(new BigDecimal("7200.00")) // rate 7.20
                .build();

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(debit, credit));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("环球外贸集团");
        when(customerMapper.selectBatchIds(any())).thenReturn(List.of(customer));

        // 即期汇率 7.25
        when(exchangeRateService.getRate(eq("USD"), any())).thenReturn(new BigDecimal("7.25000000"));

        List<LedgerCustomerBalanceRespVO> list = ledgerService.getCustomerBalanceList(101L, "USD");

        assertNotNull(list);
        assertEquals(1, list.size());
        LedgerCustomerBalanceRespVO resp = list.get(0);
        assertEquals(101L, resp.getCustomerId());
        assertEquals("环球外贸集团", resp.getCustomerName());
        assertEquals("USD", resp.getCurrency());
        assertEquals(new BigDecimal("3000.00"), resp.getTotalDebit());
        assertEquals(new BigDecimal("1000.00"), resp.getTotalCredit());
        assertEquals(new BigDecimal("2000.00"), resp.getBalance());
        assertEquals(new BigDecimal("14400.00"), resp.getHistoricalBaseBalance());
        assertEquals(new BigDecimal("7.25000000"), resp.getCurrentRate());
        assertEquals(new BigDecimal("14500.00"), resp.getCurrentBaseBalance());
        assertEquals(new BigDecimal("100.00"), resp.getExchangeDiff());
    }

    // ==================== Cycle 2 RED 测试 ====================

    @Test
    @DisplayName("Spec6: getCustomerBalanceList 仅汇总 AR 类分录，RECEIPT 现金分录不应计入 AR 余额")
    void testGetCustomerBalanceList_ExcludesReceiptCashEntries() {
        // AR_INC 借方：应收 3000
        LedgerEntryDO arInc = LedgerEntryDO.builder()
                .id(1L).partyType("CUSTOMER").partyId(101L).currency("CNY")
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .originalAmount(new BigDecimal("3000.00"))
                .baseAmount(new BigDecimal("3000.00")).build();

        // RECEIPT 贷方：现金流入 1000（不应影响 AR 余额）
        LedgerEntryDO receipt = LedgerEntryDO.builder()
                .id(2L).partyType("CUSTOMER").partyId(101L).currency("CNY")
                .category(FinanceLedgerCategoryEnum.RECEIPT.getValue())
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .originalAmount(new BigDecimal("1000.00"))
                .baseAmount(new BigDecimal("1000.00")).build();

        // RECEIPT_WRITE_OFF 贷方：冲减应收 1000
        LedgerEntryDO writeoff = LedgerEntryDO.builder()
                .id(3L).partyType("CUSTOMER").partyId(101L).currency("CNY")
                .category(FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue())
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .originalAmount(new BigDecimal("1000.00"))
                .baseAmount(new BigDecimal("1000.00")).build();

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(arInc, receipt, writeoff));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("测试客户");
        when(customerMapper.selectBatchIds(any())).thenReturn(List.of(customer));

        List<LedgerCustomerBalanceRespVO> list = ledgerService.getCustomerBalanceList(101L, "CNY");

        assertNotNull(list);
        assertEquals(1, list.size());
        LedgerCustomerBalanceRespVO resp = list.get(0);
        // totalDebit 应只含 AR_INC: 3000
        assertEquals(new BigDecimal("3000.00"), resp.getTotalDebit());
        // totalCredit 应只含 AR 类减少分录(RECEIPT_WRITE_OFF): 1000，不含 RECEIPT 现金分录
        assertEquals(new BigDecimal("1000.00"), resp.getTotalCredit());
        // balance = 3000 - 1000 = 2000
        assertEquals(new BigDecimal("2000.00"), resp.getBalance());
    }

    // ==================== Cycle 7 RED 测试 ====================

    @Test
    @DisplayName("S2: LedgerService 接口不应声明 update 或 delete 方法（ADR-0043 禁改禁删）")
    void testLedgerServiceInterface_HasNoUpdateOrDeleteMethod() {
        // 反射检查接口方法，确保不存在 update/delete
        java.lang.reflect.Method[] methods = LedgerService.class.getDeclaredMethods();
        boolean hasUpdate = java.util.Arrays.stream(methods)
                .anyMatch(m -> m.getName().equals("update"));
        boolean hasDelete = java.util.Arrays.stream(methods)
                .anyMatch(m -> m.getName().equals("delete"));
        assertFalse(hasUpdate, "LedgerService 接口不应声明 update 方法");
        assertFalse(hasDelete, "LedgerService 接口不应声明 delete 方法");
    }

}

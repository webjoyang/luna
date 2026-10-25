package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.OtherTxnDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.OtherTxnMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 其他收支单单元测试（TDD Seam 2）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class OtherTxnServiceImplTest {

    @InjectMocks
    private OtherTxnServiceImpl otherTxnService;

    @Mock
    private OtherTxnMapper otherTxnMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private FinancePostingService financePostingService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OtherTxnDO.class);
    }

    @Test
    @DisplayName("类别与方向匹配校验：类别与 direction 不符抛出 020_110")
    void testCreateOtherTxn_categoryDirectionMismatch_throws020_110() {
        // 1. 收入类别 配流出方向 CREDIT -> 抛 020_110
        OtherTxnCreateReqVO req1 = new OtherTxnCreateReqVO();
        req1.setAccountId(100L);
        req1.setDirection("CREDIT");
        req1.setCategory("INTEREST_IN");
        req1.setOriginalAmount(new BigDecimal("100.00"));
        req1.setTxnDate(LocalDate.of(2026, 10, 19));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req1));
        assertEquals(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH.getCode(), ex1.getCode());

        // 2. 支出类别 配流入方向 DEBIT -> 抛 020_110
        OtherTxnCreateReqVO req2 = new OtherTxnCreateReqVO();
        req2.setAccountId(100L);
        req2.setDirection("DEBIT");
        req2.setCategory("FEE_OUT");
        req2.setOriginalAmount(new BigDecimal("50.00"));
        req2.setTxnDate(LocalDate.of(2026, 10, 19));

        ServiceException ex2 = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req2));
        assertEquals(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH.getCode(), ex2.getCode());

        // 3. 非法类别 -> 抛 020_110
        OtherTxnCreateReqVO req3 = new OtherTxnCreateReqVO();
        req3.setAccountId(100L);
        req3.setDirection("DEBIT");
        req3.setCategory("INVALID_CATEGORY");
        req3.setOriginalAmount(new BigDecimal("50.00"));
        req3.setTxnDate(LocalDate.of(2026, 10, 19));

        ServiceException ex3 = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req3));
        assertEquals(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH.getCode(), ex3.getCode());

        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("账户不存在拦截：账户不存在抛出 020_100")
    void testCreateOtherTxn_accountNotExists_throws020_100() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(999L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        when(accountMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("账户停用拦截：账户停用抛出 020_101")
    void testCreateOtherTxn_accountDisabled_throws020_101() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setStatus(AccountServiceImpl.STATUS_DISABLE);
        when(accountMapper.selectById(100L)).thenReturn(account);

        ServiceException ex = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("其他收入成功：登记生成 QT 单，写 DEBIT / IN_OTHER 分录，人民币汇率=1")
    void testCreateOtherTxn_success_debit_inOther() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("WASTE_IN");
        req.setOriginalAmount(new BigDecimal("350.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));
        req.setRemark("处置次废布收入");

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setAccountName("建设银行基本户");
        account.setCurrency("RMB");
        account.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(account);
        when(exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 19)))
                .thenReturn(BigDecimal.ONE);

        doAnswer(inv -> {
            OtherTxnDO txn = inv.getArgument(0);
            txn.setId(501L);
            return 1;
        }).when(otherTxnMapper).insert(any(OtherTxnDO.class));

        Long txnId = otherTxnService.createOtherTxn(req);
        assertEquals(501L, txnId);

        // 1. 验证主表保存
        ArgumentCaptor<OtherTxnDO> txnCaptor = ArgumentCaptor.forClass(OtherTxnDO.class);
        verify(otherTxnMapper).insert(txnCaptor.capture());
        OtherTxnDO savedTxn = txnCaptor.getValue();
        assertEquals("建设银行基本户", savedTxn.getAccountName());
        assertEquals("DEBIT", savedTxn.getDirection());
        assertEquals("WASTE_IN", savedTxn.getCategory());
        assertEquals("RMB", savedTxn.getCurrency());
        assertEquals(new BigDecimal("350.00"), savedTxn.getOriginalAmount());
        assertEquals(BigDecimal.ONE, savedTxn.getExchangeRate());
        assertEquals(new BigDecimal("350.00"), savedTxn.getBaseAmount());
        assertTrue(savedTxn.getTxnNo().startsWith("QT20261019"));

        // 2. 验证过账：DEBIT / IN_OTHER，ref_type=OTHER_TXN
        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService).post(postingCaptor.capture());
        PostingReq posting = postingCaptor.getValue();
        assertEquals(100L, posting.getAccountId());
        assertEquals("DEBIT", posting.getDirection());
        assertEquals("IN_OTHER", posting.getCategory());
        assertEquals("OTHER_TXN", posting.getRefType());
        assertEquals(501L, posting.getRefId());
        assertEquals(savedTxn.getTxnNo(), posting.getRefNo());
        assertEquals(new BigDecimal("350.00"), posting.getOriginalAmount());
        assertEquals(BigDecimal.ONE, posting.getExchangeRate());
        assertEquals("NONE", posting.getPartyType());
    }

    @Test
    @DisplayName("其他支出成功：登记生成 QT 单，写 CREDIT / OUT_OTHER 分录")
    void testCreateOtherTxn_success_credit_outOther() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("CREDIT");
        req.setCategory("FEE_OUT");
        req.setOriginalAmount(new BigDecimal("25.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));
        req.setRemark("银行网银年费");

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setAccountName("建设银行基本户");
        account.setCurrency("RMB");
        account.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(account);
        when(exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 19)))
                .thenReturn(BigDecimal.ONE);

        doAnswer(inv -> {
            OtherTxnDO txn = inv.getArgument(0);
            txn.setId(502L);
            return 1;
        }).when(otherTxnMapper).insert(any(OtherTxnDO.class));

        Long txnId = otherTxnService.createOtherTxn(req);
        assertEquals(502L, txnId);

        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService).post(postingCaptor.capture());
        PostingReq posting = postingCaptor.getValue();
        assertEquals("CREDIT", posting.getDirection());
        assertEquals("OUT_OTHER", posting.getCategory());
        assertEquals("OTHER_TXN", posting.getRefType());
        assertEquals(502L, posting.getRefId());
    }

    @Test
    @DisplayName("外币其他收支：按业务日获取 SPOT 汇率快照并折算本位币金额")
    void testCreateOtherTxn_foreignCurrency_lookupsExchangeRate_andComputesBaseAmount() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setAccountName("花旗银行美金户");
        account.setCurrency("USD");
        account.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(account);
        when(exchangeRateService.getRate("USD", LocalDate.of(2026, 10, 19)))
                .thenReturn(new BigDecimal("7.23450000"));

        doAnswer(inv -> {
            OtherTxnDO txn = inv.getArgument(0);
            txn.setId(503L);
            return 1;
        }).when(otherTxnMapper).insert(any(OtherTxnDO.class));

        otherTxnService.createOtherTxn(req);

        ArgumentCaptor<OtherTxnDO> txnCaptor = ArgumentCaptor.forClass(OtherTxnDO.class);
        verify(otherTxnMapper).insert(txnCaptor.capture());
        OtherTxnDO saved = txnCaptor.getValue();
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("7.23450000"), saved.getExchangeRate());
        // 200.00 * 7.23450000 = 1446.90
        assertEquals(new BigDecimal("1446.90"), saved.getBaseAmount());

        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService).post(postingCaptor.capture());
        assertEquals(new BigDecimal("7.23450000"), postingCaptor.getValue().getExchangeRate());
        assertEquals("USD", postingCaptor.getValue().getCurrency());
    }

    @Test
    @DisplayName("外币无生效汇率：抛出 020_200 终止并回滚")
    void testCreateOtherTxn_foreignCurrency_rateNotFound_throws020_200() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setCurrency("EUR");
        account.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(account);
        when(exchangeRateService.getRate("EUR", LocalDate.of(2026, 10, 19)))
                .thenThrow(new ServiceException(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND));

        ServiceException ex = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("getOtherTxn: 详情可查，返回正确视图")
    void testGetOtherTxn() {
        OtherTxnDO txn = new OtherTxnDO();
        txn.setId(501L);
        txn.setTxnNo("QT20261019001");
        txn.setAccountId(100L);
        txn.setAccountName("基本户");
        txn.setDirection("DEBIT");
        txn.setCategory("INTEREST_IN");
        txn.setCurrency("RMB");
        txn.setOriginalAmount(new BigDecimal("88.00"));
        txn.setExchangeRate(BigDecimal.ONE);
        txn.setBaseAmount(new BigDecimal("88.00"));
        txn.setTxnDate(LocalDate.of(2026, 10, 19));

        when(otherTxnMapper.selectById(501L)).thenReturn(txn);

        OtherTxnRespVO vo = otherTxnService.getOtherTxn(501L);
        assertNotNull(vo);
        assertEquals("QT20261019001", vo.getTxnNo());
        assertEquals(new BigDecimal("88.00"), vo.getOriginalAmount());
    }

    @Test
    @DisplayName("横向越权防御：资金账户属于他租户或不存在时报 020_100")
    void testCreateOtherTxn_horizontalPrivilege_otherTenantAccount_throws020_100() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(8888L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        // MyBatis-Plus 租户插件在跨租户查询时查不到记录返回 null
        when(accountMapper.selectById(8888L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("单号重试耗尽：顺延重试耗尽抛出 OTHER_TXN_NO_EXHAUSTED 020_112")
    void testCreateOtherTxn_retryExhausted_throws020_112() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("200.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        AccountDO account = new AccountDO();
        account.setId(100L);
        account.setAccountName("基本户");
        account.setCurrency("RMB");
        account.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(account);
        when(exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 19))).thenReturn(BigDecimal.ONE);

        // 模拟所有候选单号已被占用
        when(otherTxnMapper.selectByTxnNoIncludingDeleted(anyString())).thenReturn(new OtherTxnDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(req));
        assertEquals(FinanceErrorCodeConstants.OTHER_TXN_NO_EXHAUSTED.getCode(), ex.getCode());
    }

}

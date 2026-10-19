package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.TransferDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.TransferMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
 * 资金账户间转账单单元测试（TDD Seam 1）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    @InjectMocks
    private TransferServiceImpl transferService;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private FinancePostingService financePostingService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TransferDO.class);
    }

    @Test
    @DisplayName("同账户转账拦截：转出账户与转入账户相同抛出 020_107")
    void testCreateTransfer_sameAccount_throws020_107() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(100L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("500.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.TRANSFER_ACCOUNT_SAME.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("跨币种转账拦截：转出/转入账户币种不一致抛出 020_106")
    void testCreateTransfer_crossCurrency_throws020_106() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(101L);
        req.setToAccountId(102L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("500.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(101L);
        fromAccount.setAccountName("人民币公户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(102L);
        toAccount.setAccountName("美元外币户");
        toAccount.setCurrency("USD");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(101L)).thenReturn(fromAccount);
        when(accountMapper.selectById(102L)).thenReturn(toAccount);

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("账户停用拦截：转账涉及停用账户抛出 020_101")
    void testCreateTransfer_disabledAccount_throws020_101() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(101L);
        req.setToAccountId(102L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("500.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(101L);
        fromAccount.setAccountName("基本户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_DISABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(102L);
        toAccount.setAccountName("备用金户");
        toAccount.setCurrency("RMB");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(101L)).thenReturn(fromAccount);
        when(accountMapper.selectById(102L)).thenReturn(toAccount);

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());
        verifyNoInteractions(financePostingService);
    }

    @Test
    @DisplayName("成功转账：两账户按 ID 升序过账、类别方向 ref 与账户名称快照正确")
    void testCreateTransfer_success_postOrderAscendingAndDirectionsCorrect() {
        // fromAccountId (200L) > toAccountId (100L) => 应先对 100L 过账，再对 200L 过账
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(200L);
        req.setToAccountId(100L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("1200.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));
        req.setRemark("从基本户调拨至零钱户");

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(200L);
        fromAccount.setAccountName("工商银行基本户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(100L);
        toAccount.setAccountName("出纳微信零钱户");
        toAccount.setCurrency("RMB");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(200L)).thenReturn(fromAccount);
        when(accountMapper.selectById(100L)).thenReturn(toAccount);

        doAnswer(inv -> {
            TransferDO t = inv.getArgument(0);
            t.setId(999L);
            return 1;
        }).when(transferMapper).insert(any(TransferDO.class));

        when(financePostingService.post(any(PostingReq.class))).thenReturn(10001L, 10002L);

        Long transferId = transferService.createTransfer(req);
        assertEquals(999L, transferId);

        // 1. 验证转账单主表落库
        ArgumentCaptor<TransferDO> transferCaptor = ArgumentCaptor.forClass(TransferDO.class);
        verify(transferMapper).insert(transferCaptor.capture());
        TransferDO savedTransfer = transferCaptor.getValue();
        assertEquals("工商银行基本户", savedTransfer.getFromAccountName());
        assertEquals("出纳微信零钱户", savedTransfer.getToAccountName());
        assertEquals("RMB", savedTransfer.getCurrency());
        assertEquals(new BigDecimal("1200.00"), savedTransfer.getAmount());
        assertEquals("COMPLETED", savedTransfer.getStatus());
        assertTrue(savedTransfer.getTransferNo().startsWith("ZZ20261019"));

        // 2. 验证两账户按 ID 升序过账顺序：100L 先过，200L 后过
        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService, times(2)).post(postingCaptor.capture());

        List<PostingReq> capturedPostings = postingCaptor.getAllValues();
        assertEquals(2, capturedPostings.size());

        // 第一次过账：账户 100L（作为 toAccount，流入 DEBIT，类别 TRANSFER_IN）
        PostingReq firstPost = capturedPostings.get(0);
        assertEquals(100L, firstPost.getAccountId());
        assertEquals("DEBIT", firstPost.getDirection());
        assertEquals("TRANSFER_IN", firstPost.getCategory());
        assertEquals("TRANSFER", firstPost.getRefType());
        assertEquals(999L, firstPost.getRefId());
        assertEquals(savedTransfer.getTransferNo(), firstPost.getRefNo());
        assertEquals(new BigDecimal("1200.00"), firstPost.getOriginalAmount());
        assertEquals("RMB", firstPost.getCurrency());

        // 第二次过账：账户 200L（作为 fromAccount，流出 CREDIT，类别 TRANSFER_OUT）
        PostingReq secondPost = capturedPostings.get(1);
        assertEquals(200L, secondPost.getAccountId());
        assertEquals("CREDIT", secondPost.getDirection());
        assertEquals("TRANSFER_OUT", secondPost.getCategory());
        assertEquals("TRANSFER", secondPost.getRefType());
        assertEquals(999L, secondPost.getRefId());
        assertEquals(savedTransfer.getTransferNo(), secondPost.getRefNo());
        assertEquals(new BigDecimal("1200.00"), secondPost.getOriginalAmount());
        assertEquals("RMB", secondPost.getCurrency());
    }

    @Test
    @DisplayName("一边失败整体回滚：一方过账抛出余额不足异常，异常直接冒泡整体回滚")
    void testCreateTransfer_oneSideFails_propagatesException() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(200L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("99999.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(100L);
        fromAccount.setAccountName("基本户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(200L);
        toAccount.setAccountName("备用金户");
        toAccount.setCurrency("RMB");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(fromAccount);
        when(accountMapper.selectById(200L)).thenReturn(toAccount);

        doAnswer(inv -> {
            TransferDO t = inv.getArgument(0);
            t.setId(888L);
            return 1;
        }).when(transferMapper).insert(any(TransferDO.class));

        // 100L 先过账，扣款余额不足抛出 023_004
        when(financePostingService.post(any(PostingReq.class)))
                .thenThrow(new ServiceException(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH));

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("外币转账：获取外币业务日汇率快照传给过账服务")
    void testCreateTransfer_foreignCurrency_lookupsExchangeRate() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(200L);
        req.setCurrency("USD");
        req.setAmount(new BigDecimal("1000.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(100L);
        fromAccount.setAccountName("美金基本户");
        fromAccount.setCurrency("USD");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(200L);
        toAccount.setAccountName("美金备用户");
        toAccount.setCurrency("USD");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(fromAccount);
        when(accountMapper.selectById(200L)).thenReturn(toAccount);

        when(exchangeRateService.getRate("USD", LocalDate.of(2026, 10, 19)))
                .thenReturn(new BigDecimal("7.25000000"));

        doAnswer(inv -> {
            TransferDO t = inv.getArgument(0);
            t.setId(777L);
            return 1;
        }).when(transferMapper).insert(any(TransferDO.class));

        transferService.createTransfer(req);

        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService, times(2)).post(postingCaptor.capture());

        for (PostingReq post : postingCaptor.getAllValues()) {
            assertEquals("USD", post.getCurrency());
            assertEquals(new BigDecimal("7.25000000"), post.getExchangeRate());
        }
    }

    @Test
    @DisplayName("getTransfer: 详情可查，返回正确视图")
    void testGetTransfer() {
        TransferDO transfer = new TransferDO();
        transfer.setId(1L);
        transfer.setTransferNo("ZZ20261019001");
        transfer.setFromAccountId(100L);
        transfer.setFromAccountName("转出户");
        transfer.setToAccountId(200L);
        transfer.setToAccountName("转入户");
        transfer.setCurrency("RMB");
        transfer.setAmount(new BigDecimal("800.00"));
        transfer.setTransferDate(LocalDate.of(2026, 10, 19));
        transfer.setStatus("COMPLETED");

        when(transferMapper.selectById(1L)).thenReturn(transfer);

        TransferRespVO vo = transferService.getTransfer(1L);
        assertNotNull(vo);
        assertEquals("ZZ20261019001", vo.getTransferNo());
        assertEquals(new BigDecimal("800.00"), vo.getAmount());
    }

    @Test
    @DisplayName("横向越权防御：转出账户属于他租户或不存在时报 020_100")
    void testCreateTransfer_horizontalPrivilege_otherTenantAccount_throws020_100() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(9999L);
        req.setToAccountId(200L);
        req.setAmount(new BigDecimal("500.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        // MyBatis-Plus 租户插件在跨租户查询时查不到记录返回 null
        when(accountMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("币种缺省：请求未传 currency 时，自动采用转出账户币种")
    void testCreateTransfer_currencyOmitted_usesFromAccountCurrency() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(200L);
        req.setCurrency(null); // 出纳不填币种，由后端根据账户自动派生
        req.setAmount(new BigDecimal("600.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(100L);
        fromAccount.setAccountName("基本户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(200L);
        toAccount.setAccountName("备用金户");
        toAccount.setCurrency("RMB");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(fromAccount);
        when(accountMapper.selectById(200L)).thenReturn(toAccount);

        doAnswer(inv -> {
            TransferDO t = inv.getArgument(0);
            t.setId(1001L);
            return 1;
        }).when(transferMapper).insert(any(TransferDO.class));

        transferService.createTransfer(req);

        ArgumentCaptor<TransferDO> captor = ArgumentCaptor.forClass(TransferDO.class);
        verify(transferMapper).insert(captor.capture());
        assertEquals("RMB", captor.getValue().getCurrency());
    }

    @Test
    @DisplayName("单号重试耗尽：顺延重试耗尽抛出 TRANSFER_NO_EXHAUSTED 020_111")
    void testCreateTransfer_retryExhausted_throws020_111() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(200L);
        req.setAmount(new BigDecimal("100.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        AccountDO fromAccount = new AccountDO();
        fromAccount.setId(100L);
        fromAccount.setAccountName("基本户");
        fromAccount.setCurrency("RMB");
        fromAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        AccountDO toAccount = new AccountDO();
        toAccount.setId(200L);
        toAccount.setAccountName("备用金户");
        toAccount.setCurrency("RMB");
        toAccount.setStatus(AccountServiceImpl.STATUS_ENABLE);

        when(accountMapper.selectById(100L)).thenReturn(fromAccount);
        when(accountMapper.selectById(200L)).thenReturn(toAccount);

        // 模拟所有候选单号已被占用
        when(transferMapper.selectByTransferNoIncludingDeleted(anyString())).thenReturn(new TransferDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> transferService.createTransfer(req));
        assertEquals(FinanceErrorCodeConstants.TRANSFER_NO_EXHAUSTED.getCode(), ex.getCode());
    }

}

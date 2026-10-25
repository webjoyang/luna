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
import java.util.Collections;
import java.util.List;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;

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

    @Mock
    private AccountMapper accountMapper;

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

    @Test
    @DisplayName("getLedgerPage: 账户不存在时抛出 1_030_020_100 ACCOUNT_NOT_EXISTS (横向越权防御)")
    void testGetLedgerPage_accountNotExists_throwsException() {
        LedgerPageReqVO reqVO = new LedgerPageReqVO();
        reqVO.setAccountId(9999L);
        when(accountMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> ledgerService.getLedgerPage(reqVO));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getLedgerPage: 正常分页查询，包含筛选透传与来源单号跳转路由解析")
    void testGetLedgerPage_success() {
        LedgerPageReqVO reqVO = new LedgerPageReqVO();
        reqVO.setAccountId(10L);
        reqVO.setCategory("OUT_EXPENSE");
        reqVO.setDirection("CREDIT");
        reqVO.setPartyName("张三");
        reqVO.setKeyword("BX");

        AccountDO account = new AccountDO();
        account.setId(10L);
        account.setAccountName("基本户");
        when(accountMapper.selectById(10L)).thenReturn(account);

        LedgerEntryDO entry1 = new LedgerEntryDO();
        entry1.setId(101L);
        entry1.setBizDate(LocalDate.of(2026, 10, 19));
        entry1.setCategory("OUT_EXPENSE");
        entry1.setDirection("CREDIT");
        entry1.setPartyType("EMPLOYEE");
        entry1.setPartyName("张三");
        entry1.setAccountId(10L);
        entry1.setCurrency("RMB");
        entry1.setExchangeRate(BigDecimal.ONE);
        entry1.setOriginalAmount(new BigDecimal("500.00"));
        entry1.setBaseAmount(new BigDecimal("500.00"));
        entry1.setRefType("EXPENSE");
        entry1.setRefId(201L);
        entry1.setRefNo("BX202610190001");
        entry1.setRemark("市内交通");

        LedgerEntryDO entry2 = new LedgerEntryDO();
        entry2.setId(102L);
        entry2.setBizDate(LocalDate.of(2026, 10, 18));
        entry2.setCategory("TRANSFER_OUT");
        entry2.setDirection("CREDIT");
        entry2.setAccountId(10L);
        entry2.setCurrency("RMB");
        entry2.setOriginalAmount(new BigDecimal("1000.00"));
        entry2.setBaseAmount(new BigDecimal("1000.00"));
        entry2.setRefType("TRANSFER");
        entry2.setRefId(301L);
        entry2.setRefNo("ZZ202610180001");

        PageResult<LedgerEntryDO> pageResult = new PageResult<>(List.of(entry1, entry2), 2L);
        when(ledgerEntryMapper.selectPage(eq(reqVO), any(LambdaQueryWrapperX.class))).thenReturn(pageResult);

        PageResult<LedgerRespVO> result = ledgerService.getLedgerPage(reqVO);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().size());

        LedgerRespVO vo1 = result.getList().get(0);
        assertEquals(101L, vo1.getId());
        assertEquals("OUT_EXPENSE", vo1.getCategory());
        assertEquals("张三", vo1.getPartyName());
        assertEquals(new BigDecimal("500.00"), vo1.getOriginalAmount());
        assertEquals("/finance/expense?id=201", vo1.getSourceRoute());

        LedgerRespVO vo2 = result.getList().get(1);
        assertEquals(102L, vo2.getId());
        assertEquals("/finance/account?tab=transfer", vo2.getSourceRoute());
    }

    @Test
    @DisplayName("getLedgerPage: 未指定 accountId 时默认资金流水视图，约束 account_id IS NOT NULL")
    void testGetLedgerPage_withoutAccountId_appliesNotNull() {
        LedgerPageReqVO reqVO = new LedgerPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<LedgerEntryDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(ledgerEntryMapper.selectPage(eq(reqVO), any(LambdaQueryWrapperX.class))).thenReturn(pageResult);

        PageResult<LedgerRespVO> result = ledgerService.getLedgerPage(reqVO);
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        verify(accountMapper, never()).selectById(any());
    }

}


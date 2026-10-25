package com.luna.lunasrv.module.lunaerp.service.finance.overview;

import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.junit.jupiter.api.BeforeEach;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.AccountSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.CategorySummaryRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.AccountDirectionAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.CategoryAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link FinanceOverviewServiceImpl} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class FinanceOverviewServiceImplTest {

    @InjectMocks
    private FinanceOverviewServiceImpl overviewService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @BeforeEach
    void setUp() {
        TableInfoTestHelper.initTableInfo(AccountDO.class);
    }

    @Test
    @DisplayName("getOverviewSummary: 跨多账户合计与合计行汇总计算自洽（期末=期初+流入-流出）")
    void testGetOverviewSummary_multiAccountAndTotal_success() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        OverviewSummaryReqVO reqVO = new OverviewSummaryReqVO();
        reqVO.setPeriodStart(start);
        reqVO.setPeriodEnd(end);

        AccountDO acc1 = new AccountDO();
        acc1.setId(1L);
        acc1.setAccountName("基本户");
        acc1.setAccountType("BANK");
        acc1.setAccountNature("CORPORATE");
        acc1.setCurrency("RMB");
        acc1.setOpeningBalance(new BigDecimal("10000.00"));
        acc1.setCurrentBalance(new BigDecimal("15000.00"));

        AccountDO acc2 = new AccountDO();
        acc2.setId(2L);
        acc2.setAccountName("微信商户");
        acc2.setAccountType("WECHAT");
        acc2.setAccountNature("CORPORATE");
        acc2.setCurrency("RMB");
        acc2.setOpeningBalance(new BigDecimal("20000.00"));
        acc2.setCurrentBalance(new BigDecimal("24000.00"));

        when(accountMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(acc1, acc2));

        // 期初前发生额
        AccountDirectionAmountDTO b1 = new AccountDirectionAmountDTO();
        b1.setAccountId(1L);
        b1.setDirection("DEBIT");
        b1.setTotalAmount(new BigDecimal("2000.00"));

        AccountDirectionAmountDTO b2 = new AccountDirectionAmountDTO();
        b2.setAccountId(1L);
        b2.setDirection("CREDIT");
        b2.setTotalAmount(new BigDecimal("1000.00"));

        AccountDirectionAmountDTO b3 = new AccountDirectionAmountDTO();
        b3.setAccountId(2L);
        b3.setDirection("DEBIT");
        b3.setTotalAmount(new BigDecimal("1000.00"));

        when(ledgerEntryMapper.selectAccountDirectionSummaryBeforeDate(null, start))
                .thenReturn(List.of(b1, b2, b3));

        // 本期发生额
        AccountDirectionAmountDTO p1 = new AccountDirectionAmountDTO();
        p1.setAccountId(1L);
        p1.setDirection("DEBIT");
        p1.setTotalAmount(new BigDecimal("5000.00"));

        AccountDirectionAmountDTO p2 = new AccountDirectionAmountDTO();
        p2.setAccountId(1L);
        p2.setDirection("CREDIT");
        p2.setTotalAmount(new BigDecimal("1000.00"));

        AccountDirectionAmountDTO p3 = new AccountDirectionAmountDTO();
        p3.setAccountId(2L);
        p3.setDirection("DEBIT");
        p3.setTotalAmount(new BigDecimal("4000.00"));

        AccountDirectionAmountDTO p4 = new AccountDirectionAmountDTO();
        p4.setAccountId(2L);
        p4.setDirection("CREDIT");
        p4.setTotalAmount(new BigDecimal("1000.00"));

        when(ledgerEntryMapper.selectAccountDirectionSummary(null, start, end))
                .thenReturn(List.of(p1, p2, p3, p4));

        // 类别汇总
        CategoryAmountDTO c1 = new CategoryAmountDTO();
        c1.setCategory("IN_CUSTOMER");
        c1.setDirection("DEBIT");
        c1.setOriginalAmount(new BigDecimal("9000.00"));
        c1.setBaseAmount(new BigDecimal("9000.00"));
        c1.setCount(3L);

        CategoryAmountDTO c2 = new CategoryAmountDTO();
        c2.setCategory("OUT_EXPENSE");
        c2.setDirection("CREDIT");
        c2.setOriginalAmount(new BigDecimal("2000.00"));
        c2.setBaseAmount(new BigDecimal("2000.00"));
        c2.setCount(2L);

        when(ledgerEntryMapper.selectCategorySummary(null, start, end))
                .thenReturn(List.of(c1, c2));

        // 执行
        OverviewSummaryRespVO resp = overviewService.getOverviewSummary(reqVO);

        assertNotNull(resp);
        assertEquals(2, resp.getAccountList().size());

        // 校验账户 1
        AccountSummaryRespVO row1 = resp.getAccountList().get(0);
        assertEquals(1L, row1.getAccountId());
        // opening = 10000 + 2000 - 1000 = 11000
        assertEquals(new BigDecimal("11000.00"), row1.getOpeningBalance());
        assertEquals(new BigDecimal("5000.00"), row1.getInflowAmount());
        assertEquals(new BigDecimal("1000.00"), row1.getOutflowAmount());
        // ending = 11000 + 5000 - 1000 = 15000
        assertEquals(new BigDecimal("15000.00"), row1.getEndingBalance());
        assertEquals(new BigDecimal("15000.00"), row1.getCurrentBalance());

        // 校验账户 2
        AccountSummaryRespVO row2 = resp.getAccountList().get(1);
        assertEquals(2L, row2.getAccountId());
        // opening = 20000 + 1000 - 0 = 21000
        assertEquals(new BigDecimal("21000.00"), row2.getOpeningBalance());
        assertEquals(new BigDecimal("4000.00"), row2.getInflowAmount());
        assertEquals(new BigDecimal("1000.00"), row2.getOutflowAmount());
        // ending = 21000 + 4000 - 1000 = 24000
        assertEquals(new BigDecimal("24000.00"), row2.getEndingBalance());
        assertEquals(new BigDecimal("24000.00"), row2.getCurrentBalance());

        // 校验合计行
        AccountSummaryRespVO total = resp.getTotal();
        assertNotNull(total);
        assertEquals("合计", total.getAccountName());
        // totalOpening = 11000 + 21000 = 32000
        assertEquals(new BigDecimal("32000.00"), total.getOpeningBalance());
        // totalInflow = 5000 + 4000 = 9000
        assertEquals(new BigDecimal("9000.00"), total.getInflowAmount());
        // totalOutflow = 1000 + 1000 = 2000
        assertEquals(new BigDecimal("2000.00"), total.getOutflowAmount());
        // totalEnding = 32000 + 9000 - 2000 = 39000
        assertEquals(new BigDecimal("39000.00"), total.getEndingBalance());
        assertEquals(new BigDecimal("39000.00"), total.getCurrentBalance());
        // 自洽校验
        assertEquals(total.getEndingBalance(), total.getOpeningBalance().add(total.getInflowAmount()).subtract(total.getOutflowAmount()));

        // 校验类别汇总与钻取
        assertEquals(2, resp.getCategoryList().size());
        CategorySummaryRespVO cat1 = resp.getCategoryList().get(0);
        assertEquals("IN_CUSTOMER", cat1.getCategory());
        assertEquals("DEBIT", cat1.getDirection());
        assertEquals(new BigDecimal("9000.00"), cat1.getDebitAmount());
        assertEquals(new BigDecimal("0.00"), cat1.getCreditAmount());
        assertEquals(new BigDecimal("9000.00"), cat1.getAmount());
        assertEquals(3L, cat1.getCount());

        CategorySummaryRespVO cat2 = resp.getCategoryList().get(1);
        assertEquals("OUT_EXPENSE", cat2.getCategory());
        assertEquals("CREDIT", cat2.getDirection());
        assertEquals(new BigDecimal("2000.00"), cat2.getCreditAmount());
        assertEquals(2L, cat2.getCount());
    }

    @Test
    @DisplayName("getOverviewSummary: 指定 accountId 且账户不存在时抛出 1_030_020_100 ACCOUNT_NOT_EXISTS (横向越权)")
    void testGetOverviewSummary_accountNotExists_throwsException() {
        OverviewSummaryReqVO reqVO = new OverviewSummaryReqVO();
        reqVO.setAccountId(8888L);
        when(accountMapper.selectById(8888L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> overviewService.getOverviewSummary(reqVO));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getGrossMargin: 有 AR 分录时正常计算经营毛利=AR_INC净额-供应商确认对账单成本")
    void testGetGrossMargin_withArData_success() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        GrossMarginReqVO reqVO = new GrossMarginReqVO();
        reqVO.setPeriodStart(start);
        reqVO.setPeriodEnd(end);

        // 存在 AR 分录
        when(ledgerEntryMapper.selectCountArEntriesByBizDateRange(start, end)).thenReturn(5L);
        when(ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange("AR_INC", start, end))
                .thenReturn(new BigDecimal("100000.00"));
        when(ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange("AR_INC_REVERSAL", start, end))
                .thenReturn(new BigDecimal("10000.00"));

        // 供应商对账单成本 (11号 Mapper mock)
        when(supplierReconMapper.selectSumConfirmedTotalAmount(any(), any()))
                .thenReturn(new BigDecimal("60000.00"));

        GrossMarginRespVO resp = overviewService.getGrossMargin(reqVO);

        assertNotNull(resp);
        assertTrue(resp.getHasArData());
        // 收入净额 = 100000 - 10000 = 90000.00
        assertEquals(new BigDecimal("90000.00"), resp.getRevenue());
        // 成本 = 60000.00
        assertEquals(new BigDecimal("60000.00"), resp.getCost());
        // 毛利 = 90000 - 60000 = 30000.00
        assertEquals(new BigDecimal("30000.00"), resp.getGrossProfit());
        // 毛利率 = 30000 / 90000 = 0.3333
        assertEquals(new BigDecimal("0.3333"), resp.getGrossMarginRate());
        assertEquals("管理参考、期间错配、非法定报表", resp.getDisclaimer());
        assertNull(resp.getEmptyStateNotice());
    }

    @Test
    @DisplayName("getGrossMargin: 无 AR 分录时收入区空态说明且不报错，永远返回 200")
    void testGetGrossMargin_noArData_emptyState() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        GrossMarginReqVO reqVO = new GrossMarginReqVO();
        reqVO.setPeriodStart(start);
        reqVO.setPeriodEnd(end);

        // 无 AR 分录
        when(ledgerEntryMapper.selectCountArEntriesByBizDateRange(start, end)).thenReturn(0L);
        when(supplierReconMapper.selectSumConfirmedTotalAmount(any(), any()))
                .thenReturn(new BigDecimal("5000.00"));

        GrossMarginRespVO resp = overviewService.getGrossMargin(reqVO);

        assertNotNull(resp);
        assertFalse(resp.getHasArData());
        assertEquals(new BigDecimal("0.00"), resp.getRevenue());
        assertEquals(new BigDecimal("5000.00"), resp.getCost());
        assertEquals(new BigDecimal("-5000.00"), resp.getGrossProfit());
        assertEquals(new BigDecimal("0.0000"), resp.getGrossMarginRate());
        assertEquals("管理参考、期间错配、非法定报表", resp.getDisclaimer());
        assertEquals("应收侧暂无分录数据（待发货审核事件自动落账）", resp.getEmptyStateNotice());
    }

}

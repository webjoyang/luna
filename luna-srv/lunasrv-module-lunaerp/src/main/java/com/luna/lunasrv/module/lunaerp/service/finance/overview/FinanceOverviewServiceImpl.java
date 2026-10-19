package com.luna.lunasrv.module.lunaerp.service.finance.overview;

import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
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
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 财务管理 - 收支总览 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class FinanceOverviewServiceImpl implements FinanceOverviewService {

    public static final String DIRECTION_DEBIT = "DEBIT";
    public static final String DIRECTION_CREDIT = "CREDIT";
    public static final String DISCLAIMER_TEXT = "管理参考、期间错配、非法定报表";
    public static final String EMPTY_AR_NOTICE = "应收侧暂无分录数据（待发货审核事件自动落账）";

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Override
    public OverviewSummaryRespVO getOverviewSummary(OverviewSummaryReqVO reqVO) {
        // 0. 期间缺省（若未传则缺省本月）
        LocalDate start = reqVO.getPeriodStart();
        LocalDate end = reqVO.getPeriodEnd();
        if (start == null && end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = now.withDayOfMonth(now.lengthOfMonth());
        }

        // 1. 账户校验与候选集加载
        List<AccountDO> accounts;
        if (reqVO.getAccountId() != null) {
            AccountDO account = accountMapper.selectById(reqVO.getAccountId());
            if (account == null) {
                throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
            }
            accounts = Collections.singletonList(account);
        } else {
            accounts = accountMapper.selectList(new LambdaQueryWrapperX<AccountDO>()
                    .orderByAsc(AccountDO::getId));
        }

        // 2. 批量聚合期初前历史累计发生额（防 N+1）
        List<AccountDirectionAmountDTO> beforeList = Collections.emptyList();
        if (start != null) {
            beforeList = ledgerEntryMapper.selectAccountDirectionSummaryBeforeDate(reqVO.getAccountId(), start);
        }
        Map<Long, Map<String, BigDecimal>> beforeMap = new HashMap<>();
        for (AccountDirectionAmountDTO dto : beforeList) {
            beforeMap.computeIfAbsent(dto.getAccountId(), k -> new HashMap<>())
                    .put(dto.getDirection(), dto.getTotalAmount());
        }

        // 3. 批量聚合本期发生额（防 N+1）
        List<AccountDirectionAmountDTO> inPeriodList = ledgerEntryMapper.selectAccountDirectionSummary(
                reqVO.getAccountId(), start, end);
        Map<Long, Map<String, BigDecimal>> inPeriodMap = new HashMap<>();
        for (AccountDirectionAmountDTO dto : inPeriodList) {
            inPeriodMap.computeIfAbsent(dto.getAccountId(), k -> new HashMap<>())
                    .put(dto.getDirection(), dto.getTotalAmount());
        }

        // 4. 按账户构建汇总行
        List<AccountSummaryRespVO> accountList = new ArrayList<>();
        BigDecimal totalOpening = BigDecimal.ZERO;
        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalOutflow = BigDecimal.ZERO;
        BigDecimal totalEnding = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        for (AccountDO acc : accounts) {
            AccountSummaryRespVO vo = new AccountSummaryRespVO();
            vo.setAccountId(acc.getId());
            vo.setAccountName(acc.getAccountName());
            vo.setAccountType(acc.getAccountType());
            vo.setAccountNature(acc.getAccountNature());
            vo.setCurrency(acc.getCurrency());

            BigDecimal initialOpening = acc.getOpeningBalance() != null ? acc.getOpeningBalance() : BigDecimal.ZERO;
            Map<String, BigDecimal> before = beforeMap.getOrDefault(acc.getId(), Collections.emptyMap());
            BigDecimal debitBefore = before.getOrDefault(DIRECTION_DEBIT, BigDecimal.ZERO);
            BigDecimal creditBefore = before.getOrDefault(DIRECTION_CREDIT, BigDecimal.ZERO);
            BigDecimal opening = initialOpening.add(debitBefore).subtract(creditBefore).setScale(2, RoundingMode.HALF_UP);
            vo.setOpeningBalance(opening);

            Map<String, BigDecimal> inPeriod = inPeriodMap.getOrDefault(acc.getId(), Collections.emptyMap());
            BigDecimal inflow = inPeriod.getOrDefault(DIRECTION_DEBIT, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal outflow = inPeriod.getOrDefault(DIRECTION_CREDIT, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            vo.setInflowAmount(inflow);
            vo.setOutflowAmount(outflow);

            BigDecimal ending = opening.add(inflow).subtract(outflow).setScale(2, RoundingMode.HALF_UP);
            vo.setEndingBalance(ending);

            BigDecimal current = acc.getCurrentBalance() != null ? acc.getCurrentBalance().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            vo.setCurrentBalance(current);

            accountList.add(vo);

            totalOpening = totalOpening.add(opening);
            totalInflow = totalInflow.add(inflow);
            totalOutflow = totalOutflow.add(outflow);
            totalEnding = totalEnding.add(ending);
            totalCurrent = totalCurrent.add(current);
        }

        // 5. 合计行构建（口径自洽：期末 = 期初 + 流入 - 流出）
        AccountSummaryRespVO total = new AccountSummaryRespVO();
        total.setAccountName("合计");
        total.setOpeningBalance(totalOpening.setScale(2, RoundingMode.HALF_UP));
        total.setInflowAmount(totalInflow.setScale(2, RoundingMode.HALF_UP));
        total.setOutflowAmount(totalOutflow.setScale(2, RoundingMode.HALF_UP));
        total.setEndingBalance(totalEnding.setScale(2, RoundingMode.HALF_UP));
        total.setCurrentBalance(totalCurrent.setScale(2, RoundingMode.HALF_UP));

        // 6. 类别借贷发生额汇总（用于钻取流水）
        List<CategoryAmountDTO> catList = ledgerEntryMapper.selectCategorySummary(
                reqVO.getAccountId(), start, end);
        Map<String, CategorySummaryRespVO> categoryMap = new LinkedHashMap<>();
        for (CategoryAmountDTO dto : catList) {
            CategorySummaryRespVO catVO = categoryMap.computeIfAbsent(dto.getCategory(), cat -> {
                CategorySummaryRespVO c = new CategorySummaryRespVO();
                c.setCategory(cat);
                c.setDebitAmount(BigDecimal.ZERO.setScale(2));
                c.setCreditAmount(BigDecimal.ZERO.setScale(2));
                c.setAmount(BigDecimal.ZERO.setScale(2));
                c.setNetAmount(BigDecimal.ZERO.setScale(2));
                c.setCount(0L);
                return c;
            });
            BigDecimal base = dto.getBaseAmount() != null ? dto.getBaseAmount() : BigDecimal.ZERO;
            long count = dto.getCount() != null ? dto.getCount() : 0L;
            catVO.setCount(catVO.getCount() + count);
            if (DIRECTION_DEBIT.equalsIgnoreCase(dto.getDirection())) {
                catVO.setDebitAmount(catVO.getDebitAmount().add(base).setScale(2, RoundingMode.HALF_UP));
            } else if (DIRECTION_CREDIT.equalsIgnoreCase(dto.getDirection())) {
                catVO.setCreditAmount(catVO.getCreditAmount().add(base).setScale(2, RoundingMode.HALF_UP));
            }
        }

        for (CategorySummaryRespVO catVO : categoryMap.values()) {
            catVO.setAmount(catVO.getDebitAmount().add(catVO.getCreditAmount()).setScale(2, RoundingMode.HALF_UP));
            catVO.setNetAmount(catVO.getDebitAmount().subtract(catVO.getCreditAmount()).setScale(2, RoundingMode.HALF_UP));
            catVO.setDirection(catVO.getDebitAmount().compareTo(catVO.getCreditAmount()) >= 0 ? DIRECTION_DEBIT : DIRECTION_CREDIT);
        }
        List<CategorySummaryRespVO> categoryList = new ArrayList<>(categoryMap.values());

        // 7. 组装返回 VO
        OverviewSummaryRespVO resp = new OverviewSummaryRespVO();
        resp.setPeriodStart(start);
        resp.setPeriodEnd(end);
        resp.setAccountId(reqVO.getAccountId());
        resp.setAccountList(accountList);
        resp.setTotal(total);
        resp.setCategoryList(categoryList);
        return resp;
    }

    @Override
    public GrossMarginRespVO getGrossMargin(GrossMarginReqVO reqVO) {
        // 0. 期间缺省（若未传则缺省本月）
        LocalDate start = reqVO.getPeriodStart();
        LocalDate end = reqVO.getPeriodEnd();
        if (start == null && end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = now.withDayOfMonth(now.lengthOfMonth());
        }

        // 1. 统计应收侧分录（发货日落区间 AR_INC - AR_INC_REVERSAL）
        Long arCount = ledgerEntryMapper.selectCountArEntriesByBizDateRange(start, end);
        boolean hasArData = arCount != null && arCount > 0L;

        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        String emptyStateNotice = null;
        if (hasArData) {
            BigDecimal arInc = ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange(
                    LedgerCategory.AR_INC.getValue(), start, end);
            BigDecimal arIncReversal = ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange(
                    LedgerCategory.AR_INC_REVERSAL.getValue(), start, end);
            BigDecimal arIncVal = arInc != null ? arInc : BigDecimal.ZERO;
            BigDecimal arIncRevVal = arIncReversal != null ? arIncReversal : BigDecimal.ZERO;
            revenue = arIncVal.subtract(arIncRevVal).setScale(2, RoundingMode.HALF_UP);
        } else {
            emptyStateNotice = EMPTY_AR_NOTICE;
        }

        // 2. 统计供应商对账单成本（确认日落区间 11 号 confirmed 对账单 total_amount）
        LocalDateTime startTime = start != null ? start.atStartOfDay() : null;
        LocalDateTime endTime = end != null ? end.atTime(23, 59, 59, 999999999) : null;
        BigDecimal costSum = supplierReconMapper.selectSumConfirmedTotalAmount(startTime, endTime);
        BigDecimal cost = costSum != null ? costSum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);

        // 3. 毛利差额与毛利率计算
        BigDecimal grossProfit = revenue.subtract(cost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossMarginRate;
        if (revenue.compareTo(BigDecimal.ZERO) <= 0) {
            grossMarginRate = BigDecimal.ZERO.setScale(4);
        } else {
            grossMarginRate = grossProfit.divide(revenue, 4, RoundingMode.HALF_UP);
        }

        // 4. 组装返回
        GrossMarginRespVO resp = new GrossMarginRespVO();
        resp.setPeriodStart(start);
        resp.setPeriodEnd(end);
        resp.setRevenue(revenue);
        resp.setCost(cost);
        resp.setGrossProfit(grossProfit);
        resp.setGrossMarginRate(grossMarginRate);
        resp.setHasArData(hasArData);
        resp.setDisclaimer(DISCLAIMER_TEXT);
        resp.setEmptyStateNotice(emptyStateNotice);
        return resp;
    }

}

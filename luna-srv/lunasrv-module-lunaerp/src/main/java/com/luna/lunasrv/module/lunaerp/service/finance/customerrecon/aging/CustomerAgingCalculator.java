package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FIFO 账龄纯函数计算器
 *
 * <p>核心口径（对齐 ADR-0017 / ADR-0037 与 Issue 09）：</p>
 * <ol>
 *   <li>按客户独立计算，不跨客户互抵。</li>
 *   <li>所有应收减少分录（RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL）
 *       按发生时间从最老 AR_INC 逐笔消耗。</li>
 *   <li>红冲分录 AR_INC_REVERSAL 优先对冲对应的 AR_INC 原单（reversalOfId），若无关联单号则按应收减少处理。</li>
 *   <li>账龄 = 查询日 − 发货日，严格落在五桶：0–30 / 31–60 / 61–90 / 91–180 / 181+。</li>
 *   <li>逾期判定：发货日 + credit_days < 查询日；未设 credit_days（空值或<=0）不判逾期。</li>
 *   <li>预收余额（ADVANCE 入账 − ADVANCE_WRITE_OFF）单列不进桶。</li>
 *   <li>纯函数实时计算，零状态，不落快照表。</li>
 * </ol>
 *
 * @author luna
 */
public class CustomerAgingCalculator {

    public static final String BUCKET_0_TO_30 = "0-30";
    public static final String BUCKET_31_TO_60 = "31-60";
    public static final String BUCKET_61_TO_90 = "61-90";
    public static final String BUCKET_91_TO_180 = "91-180";
    public static final String BUCKET_181_PLUS = "181+";

    /**
     * 针对单个客户，根据账龄基准日与分录列表纯函数计算账龄汇总及明细
     *
     * @param customer 客户实体（包含 creditDays, salespersonUserId 等）
     * @param currency 币种
     * @param agingDate 账龄基准日（查询日）
     * @param customerEntries 该客户的所有分录（截止到 agingDate）
     * @param advanceBalance 预收余额（单列不进桶）
     * @param refNoResolver 业务单据号解析器（可选，用于填充 refNo）
     * @return 包含 summary 与 detail 列表的结果对
     */
    public static AgingCalculationResult calculate(
            CustomerDO customer,
            String currency,
            LocalDate agingDate,
            List<LedgerEntryDO> customerEntries,
            BigDecimal advanceBalance,
            java.util.function.Function<LedgerEntryDO, String> refNoResolver) {

        if (customer == null) {
            return new AgingCalculationResult(null, Collections.emptyList());
        }
        if (agingDate == null) {
            agingDate = LocalDate.now();
        }
        if (advanceBalance == null) {
            advanceBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 1. 过滤截止到 agingDate 且币种匹配的分录
        List<LedgerEntryDO> filteredEntries = new ArrayList<>();
        if (CollUtil.isNotEmpty(customerEntries)) {
            for (LedgerEntryDO e : customerEntries) {
                if (e.getBizDate() != null && !e.getBizDate().isAfter(agingDate)) {
                    if (StrUtil.isBlank(currency) || currency.equalsIgnoreCase(e.getCurrency())) {
                        filteredEntries.add(e);
                    }
                }
            }
        }

        // 2. 收集 AR_INC 分录（按发生时间升序排列：最老在前）
        List<LedgerEntryDO> arIncEntries = filteredEntries.stream()
                .filter(e -> FinanceLedgerCategoryEnum.AR_INC.getValue().equalsIgnoreCase(e.getCategory()))
                .sorted(Comparator.comparing(LedgerEntryDO::getBizDate)
                        .thenComparing(e -> e.getId() != null ? e.getId() : 0L))
                .collect(Collectors.toList());

        // 收集针对特定 AR_INC 的红冲（AR_INC_REVERSAL）
        Map<Long, BigDecimal> arIncReversals = new HashMap<>();
        for (LedgerEntryDO e : filteredEntries) {
            if (FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue().equalsIgnoreCase(e.getCategory())) {
                if (e.getReversalOfId() != null) {
                    BigDecimal revAmt = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                    arIncReversals.merge(e.getReversalOfId(), revAmt, BigDecimal::add);
                }
            }
        }

        // 计算每笔 AR_INC 的有效原额（扣除红冲）
        List<ArIncState> arIncStates = new ArrayList<>();
        for (LedgerEntryDO ar : arIncEntries) {
            BigDecimal orig = ar.getOriginalAmount() != null ? ar.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal rev = arIncReversals.getOrDefault(ar.getId(), BigDecimal.ZERO);
            BigDecimal effective = orig.subtract(rev);
            if (effective.compareTo(BigDecimal.ZERO) > 0) {
                arIncStates.add(new ArIncState(ar, effective));
            }
        }

        // 3. 计算所有应收减少分录的总额（RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL）
        // 以及无 reversalOfId 的 AR_INC_REVERSAL
        BigDecimal totalReduction = BigDecimal.ZERO;
        for (LedgerEntryDO e : filteredEntries) {
            String cat = e.getCategory();
            if (FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue().equalsIgnoreCase(cat)
                    || FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue().equalsIgnoreCase(cat)
                    || FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                BigDecimal amt = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                totalReduction = totalReduction.add(amt);
            } else if (FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue().equalsIgnoreCase(cat)
                    && e.getReversalOfId() == null) {
                BigDecimal amt = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                totalReduction = totalReduction.add(amt);
            }
        }

        // 4. FIFO 逐笔消耗：从最老 AR_INC 开始消耗
        BigDecimal remainingReduction = totalReduction;
        for (ArIncState state : arIncStates) {
            if (remainingReduction.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (remainingReduction.compareTo(state.remainingAmount) >= 0) {
                // 当前笔被全部消耗
                remainingReduction = remainingReduction.subtract(state.remainingAmount);
                state.consumedAmount = state.consumedAmount.add(state.remainingAmount);
                state.remainingAmount = BigDecimal.ZERO;
            } else {
                // 当前笔部分消耗
                state.consumedAmount = state.consumedAmount.add(remainingReduction);
                state.remainingAmount = state.remainingAmount.subtract(remainingReduction);
                remainingReduction = BigDecimal.ZERO;
            }
        }

        // 5. 聚合各桶金额、逾期金额及未核销明细
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal bucket0To30 = BigDecimal.ZERO;
        BigDecimal bucket31To60 = BigDecimal.ZERO;
        BigDecimal bucket61To90 = BigDecimal.ZERO;
        BigDecimal bucket91To180 = BigDecimal.ZERO;
        BigDecimal bucket181Plus = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        List<CustomerAgingDetailRespVO> detailList = new ArrayList<>();
        Integer creditDays = customer.getCreditDays();

        for (ArIncState state : arIncStates) {
            if (state.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 已全额核销，不进入未核销明细及桶
            }

            BigDecimal unsettled = state.remainingAmount.setScale(2, RoundingMode.HALF_UP);
            totalBalance = totalBalance.add(unsettled);

            LocalDate bizDate = state.entry.getBizDate();
            long agingDays = ChronoUnit.DAYS.between(bizDate, agingDate);
            if (agingDays < 0) {
                agingDays = 0;
            }

            // 五桶分配
            String bucket;
            if (agingDays <= 30) {
                bucket = BUCKET_0_TO_30;
                bucket0To30 = bucket0To30.add(unsettled);
            } else if (agingDays <= 60) {
                bucket = BUCKET_31_TO_60;
                bucket31To60 = bucket31To60.add(unsettled);
            } else if (agingDays <= 90) {
                bucket = BUCKET_61_TO_90;
                bucket61To90 = bucket61To90.add(unsettled);
            } else if (agingDays <= 180) {
                bucket = BUCKET_91_TO_180;
                bucket91To180 = bucket91To180.add(unsettled);
            } else {
                bucket = BUCKET_181_PLUS;
                bucket181Plus = bucket181Plus.add(unsettled);
            }

            // 逾期判定：发货日 + credit_days < 查询日；未设 credit_days（空值或<=0）不判逾期
            boolean isOverdue = false;
            long overdueDays = 0;
            LocalDate dueDate = null;
            if (creditDays != null && creditDays > 0) {
                dueDate = bizDate.plusDays(creditDays);
                if (dueDate.isBefore(agingDate)) {
                    isOverdue = true;
                    overdueDays = ChronoUnit.DAYS.between(dueDate, agingDate);
                    overdueAmount = overdueAmount.add(unsettled);
                }
            }

            String refNo = refNoResolver != null ? refNoResolver.apply(state.entry) : null;
            String summary = StrUtil.isNotBlank(state.entry.getRemark()) ? state.entry.getRemark() : "发货应收成立";

            CustomerAgingDetailRespVO detailVO = CustomerAgingDetailRespVO.builder()
                    .ledgerEntryId(state.entry.getId())
                    .bizDate(bizDate)
                    .refType(state.entry.getRefType())
                    .refId(state.entry.getRefId())
                    .refNo(refNo)
                    .originalAmount(state.effectiveInitialAmount.setScale(2, RoundingMode.HALF_UP))
                    .settledAmount(state.consumedAmount.setScale(2, RoundingMode.HALF_UP))
                    .unsettledAmount(unsettled)
                    .currency(state.entry.getCurrency())
                    .agingDays(agingDays)
                    .bucket(bucket)
                    .creditDays(creditDays)
                    .dueDate(dueDate)
                    .isOverdue(isOverdue)
                    .overdueDays(overdueDays)
                    .summary(summary)
                    .build();

            detailList.add(detailVO);
        }

        // 6. 构造汇总 VO
        CustomerAgingSummaryRespVO summaryVO = CustomerAgingSummaryRespVO.builder()
                .customerId(customer.getId())
                .customerName(customer.getCompanyName())
                .currency(StrUtil.isNotBlank(currency) ? currency : "CNY")
                .creditDays(creditDays)
                .salesmanId(customer.getSalespersonUserId())
                .totalBalance(totalBalance.setScale(2, RoundingMode.HALF_UP))
                .bucket0To30(bucket0To30.setScale(2, RoundingMode.HALF_UP))
                .bucket31To60(bucket31To60.setScale(2, RoundingMode.HALF_UP))
                .bucket61To90(bucket61To90.setScale(2, RoundingMode.HALF_UP))
                .bucket91To180(bucket91To180.setScale(2, RoundingMode.HALF_UP))
                .bucket181Plus(bucket181Plus.setScale(2, RoundingMode.HALF_UP))
                .overdueAmount(overdueAmount.setScale(2, RoundingMode.HALF_UP))
                .advanceBalance(advanceBalance.setScale(2, RoundingMode.HALF_UP))
                .build();

        return new AgingCalculationResult(summaryVO, detailList);
    }

    public static class ArIncState {
        public final LedgerEntryDO entry;
        public final BigDecimal effectiveInitialAmount;
        public BigDecimal consumedAmount = BigDecimal.ZERO;
        public BigDecimal remainingAmount;

        public ArIncState(LedgerEntryDO entry, BigDecimal effectiveInitialAmount) {
            this.entry = entry;
            this.effectiveInitialAmount = effectiveInitialAmount;
            this.remainingAmount = effectiveInitialAmount;
        }
    }

    public static class AgingCalculationResult {
        private final CustomerAgingSummaryRespVO summary;
        private final List<CustomerAgingDetailRespVO> details;

        public AgingCalculationResult(CustomerAgingSummaryRespVO summary, List<CustomerAgingDetailRespVO> details) {
            this.summary = summary;
            this.details = details;
        }

        public CustomerAgingSummaryRespVO getSummary() {
            return summary;
        }

        public List<CustomerAgingDetailRespVO> getDetails() {
            return details;
        }
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 客户 FIFO 账龄纯函数计算器（纯领域计算，不含数据库与外部依赖）
 *
 * <p>
 * 规约：
 * 1. FIFO 消耗：所有应收减少分录（RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL）
 *    按发生时间逐笔消耗最老 AR_INC；
 * 2. 五桶区间：0–30 / 31–60 / 61–90 / 91–180 / 181+；账龄天数 = 查询基准日 − 发货日；
 * 3. 逾期判定：发货日 + credit_days < 查询日；credit_days 为空时不判逾期；
 * 4. 预收余额单列展示，不进桶，不参与应收冲抵；
 * 5. 金额计算一律使用 BigDecimal 并保留两位小数（HALF_UP）。
 * </p>
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
     * 应收增加分录（AR_INC）模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArEntry {
        private Long ledgerEntryId;
        private LocalDate bizDate;
        private BigDecimal originalAmount;
        private String refType;
        private Long refId;
        private String refNo;
        private String summary;
        private BigDecimal reversalAmount;

        public ArEntry(Long ledgerEntryId, LocalDate bizDate, BigDecimal originalAmount,
                       String refType, Long refId, String refNo, String summary) {
            this(ledgerEntryId, bizDate, originalAmount, refType, refId, refNo, summary, BigDecimal.ZERO);
        }

        public BigDecimal getNetOriginalAmount() {
            BigDecimal orig = originalAmount != null ? originalAmount : BigDecimal.ZERO;
            BigDecimal rev = reversalAmount != null ? reversalAmount : BigDecimal.ZERO;
            BigDecimal net = orig.subtract(rev);
            return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
        }
    }

    /**
     * 应收减少分录（RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL）模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditReduction {
        private Long ledgerEntryId;
        private LocalDate bizDate;
        private String category;
        private BigDecimal amount;
    }

    /**
     * 账龄未核销明细行模型（下钻项）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgingDetailItem {
        private Long ledgerEntryId;
        private LocalDate bizDate;
        private String refType;
        private Long refId;
        private String refNo;
        private String summary;
        private BigDecimal originalAmount;
        private BigDecimal writtenOffAmount;
        private BigDecimal remainingAmount;
        private int agingDays;
        private String bucket;
        private Integer creditDays;
        private LocalDate dueDate;
        private Boolean isOverdue;
        private int overdueDays;
    }

    /**
     * 账龄计算汇总结果模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgingResult {
        private Long customerId;
        private String customerName;
        private Integer creditDays;
        private LocalDate agingDate;
        private BigDecimal totalArBalance;
        private BigDecimal bucket0To30;
        private BigDecimal bucket31To60;
        private BigDecimal bucket61To90;
        private BigDecimal bucket91To180;
        private BigDecimal bucket181Plus;
        private BigDecimal advanceBalance;
        private BigDecimal overdueAmount;
        private List<AgingDetailItem> detailItems;
    }

    /**
     * 纯函数：计算指定客户在查询基准日的 FIFO 账龄
     *
     * @param customerId 客户ID
     * @param customerName 客户名称
     * @param creditDays 信用账期天数（可空，空时不判逾期）
     * @param agingDate 查询基准日（非空）
     * @param arEntries 应收增加分录列表
     * @param creditReductions 应收减少分录列表
     * @param advanceBalance 预收余额（单列，不进桶）
     * @return 账龄汇总与下钻明细结果
     */
    public static AgingResult calculate(Long customerId,
                                        String customerName,
                                        Integer creditDays,
                                        LocalDate agingDate,
                                        List<ArEntry> arEntries,
                                        List<CreditReduction> creditReductions,
                                        BigDecimal advanceBalance) {
        LocalDate queryDate = agingDate != null ? agingDate : LocalDate.now();

        // 1. 过滤并排序 AR_INC 分录（仅取 bizDate <= queryDate 且 netOriginalAmount > 0，按 bizDate ASC, id ASC 排序）
        List<ArEntry> validArEntries = new ArrayList<>();
        if (CollUtil.isNotEmpty(arEntries)) {
            for (ArEntry entry : arEntries) {
                if (entry != null && entry.getBizDate() != null && !entry.getBizDate().isAfter(queryDate)) {
                    if (entry.getNetOriginalAmount().compareTo(BigDecimal.ZERO) > 0) {
                        validArEntries.add(entry);
                    }
                }
            }
        }
        validArEntries.sort(Comparator.comparing(ArEntry::getBizDate)
                .thenComparing(e -> e.getLedgerEntryId() != null ? e.getLedgerEntryId() : 0L));

        // 2. 统计并排序应收减少分录（仅取 bizDate <= queryDate，按 bizDate ASC, id ASC 排序）
        BigDecimal totalReduction = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(creditReductions)) {
            for (CreditReduction red : creditReductions) {
                if (red != null && red.getBizDate() != null && !red.getBizDate().isAfter(queryDate)) {
                    BigDecimal amt = red.getAmount() != null ? red.getAmount() : BigDecimal.ZERO;
                    if (amt.compareTo(BigDecimal.ZERO) > 0) {
                        totalReduction = totalReduction.add(amt);
                    }
                }
            }
        }

        // 3. FIFO 冲抵计算：按发生时间逐笔消耗最老 AR_INC
        BigDecimal remainingWriteOff = totalReduction;
        List<AgingDetailItem> detailItems = new ArrayList<>();

        BigDecimal bucket0To30 = BigDecimal.ZERO;
        BigDecimal bucket31To60 = BigDecimal.ZERO;
        BigDecimal bucket61To90 = BigDecimal.ZERO;
        BigDecimal bucket91To180 = BigDecimal.ZERO;
        BigDecimal bucket181Plus = BigDecimal.ZERO;
        BigDecimal totalArBalance = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (ArEntry entry : validArEntries) {
            BigDecimal netAmount = entry.getNetOriginalAmount();
            BigDecimal consumed = netAmount.min(remainingWriteOff);
            remainingWriteOff = remainingWriteOff.subtract(consumed);
            BigDecimal remaining = netAmount.subtract(consumed);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                // 计算账龄天数 = queryDate - bizDate
                long daysBetween = ChronoUnit.DAYS.between(entry.getBizDate(), queryDate);
                int agingDays = (int) Math.max(0, daysBetween);

                // 划分五桶
                String bucket;
                if (agingDays <= 30) {
                    bucket = BUCKET_0_TO_30;
                    bucket0To30 = bucket0To30.add(remaining);
                } else if (agingDays <= 60) {
                    bucket = BUCKET_31_TO_60;
                    bucket31To60 = bucket31To60.add(remaining);
                } else if (agingDays <= 90) {
                    bucket = BUCKET_61_TO_90;
                    bucket61To90 = bucket61To90.add(remaining);
                } else if (agingDays <= 180) {
                    bucket = BUCKET_91_TO_180;
                    bucket91To180 = bucket91To180.add(remaining);
                } else {
                    bucket = BUCKET_181_PLUS;
                    bucket181Plus = bucket181Plus.add(remaining);
                }

                totalArBalance = totalArBalance.add(remaining);

                // 逾期判定：发货日 + credit_days < 查询日；credit_days 空值不判逾期
                boolean isOverdue = false;
                LocalDate dueDate = null;
                int overdueDays = 0;
                if (creditDays != null && creditDays >= 0) {
                    dueDate = entry.getBizDate().plusDays(creditDays);
                    if (dueDate.isBefore(queryDate)) {
                        isOverdue = true;
                        overdueDays = (int) ChronoUnit.DAYS.between(dueDate, queryDate);
                        overdueAmount = overdueAmount.add(remaining);
                    }
                }

                BigDecimal reversalAmt = entry.getReversalAmount() != null ? entry.getReversalAmount() : BigDecimal.ZERO;
                BigDecimal totalWrittenOff = consumed.add(reversalAmt);

                detailItems.add(AgingDetailItem.builder()
                        .ledgerEntryId(entry.getLedgerEntryId())
                        .bizDate(entry.getBizDate())
                        .refType(entry.getRefType())
                        .refId(entry.getRefId())
                        .refNo(entry.getRefNo())
                        .summary(entry.getSummary())
                        .originalAmount(entry.getOriginalAmount() != null ? entry.getOriginalAmount().setScale(2, RoundingMode.HALF_UP) : netAmount.setScale(2, RoundingMode.HALF_UP))
                        .writtenOffAmount(totalWrittenOff.setScale(2, RoundingMode.HALF_UP))
                        .remainingAmount(remaining.setScale(2, RoundingMode.HALF_UP))
                        .agingDays(agingDays)
                        .bucket(bucket)
                        .creditDays(creditDays)
                        .dueDate(dueDate)
                        .isOverdue(isOverdue)
                        .overdueDays(overdueDays)
                        .build());
            }
        }

        BigDecimal advBalance = advanceBalance != null && advanceBalance.compareTo(BigDecimal.ZERO) > 0
                ? advanceBalance.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return AgingResult.builder()
                .customerId(customerId)
                .customerName(customerName)
                .creditDays(creditDays)
                .agingDate(queryDate)
                .totalArBalance(totalArBalance.setScale(2, RoundingMode.HALF_UP))
                .bucket0To30(bucket0To30.setScale(2, RoundingMode.HALF_UP))
                .bucket31To60(bucket31To60.setScale(2, RoundingMode.HALF_UP))
                .bucket61To90(bucket61To90.setScale(2, RoundingMode.HALF_UP))
                .bucket91To180(bucket91To180.setScale(2, RoundingMode.HALF_UP))
                .bucket181Plus(bucket181Plus.setScale(2, RoundingMode.HALF_UP))
                .advanceBalance(advBalance)
                .overdueAmount(overdueAmount.setScale(2, RoundingMode.HALF_UP))
                .detailItems(detailItems)
                .build();
    }
}

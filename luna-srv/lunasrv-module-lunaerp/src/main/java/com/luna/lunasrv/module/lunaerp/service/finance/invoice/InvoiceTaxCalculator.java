package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.INVOICE_TAX_RATE_INVALID;

/**
 * 发票税额纯函数计算器
 *
 * <p>
 * 规约：
 * 1. 税率支持数值百分比（如 13.00, 9.00, 6.00, 3.00, 1.00, 0.00）及 "EXEMPT" / "免税"（按 0.00% 处理）；
 * 2. 负数税率或超过 100% 抛出 INVOICE_TAX_RATE_INVALID；
 * 3. 服务端税额反算公式：
 *    total_amount_excl_tax = total_amount_incl_tax / (1 + tax_rate / 100)（保留两位小数，HALF_UP）；
 *    total_tax = total_amount_incl_tax - total_amount_excl_tax；
 *    保证 不含税金额 + 税额 == 价税合计（恒等式成立，避免舍入误差导致差 1 分钱）。
 * </p>
 *
 * @author luna
 */
public class InvoiceTaxCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxCalculationResult {
        /**
         * 不含税金额合计
         */
        private BigDecimal totalAmountExclTax;
        /**
         * 税额合计
         */
        private BigDecimal totalTax;
        /**
         * 价税合计总金额
         */
        private BigDecimal totalAmountInclTax;
        /**
         * 税率数值（如 13.00 表示 13%）
         */
        private BigDecimal taxRate;
    }

    /**
     * 解析税率字符串为 BigDecimal（百分比数值）
     *
     * @param taxRateStr 税率字符串，如 "13.00", "13%", "EXEMPT", "免税"
     * @return 税率百分比数值
     */
    public static BigDecimal parseTaxRate(String taxRateStr) {
        if (StrUtil.isBlank(taxRateStr)) {
            throw ServiceExceptionUtil.exception(INVOICE_TAX_RATE_INVALID);
        }
        String cleanStr = taxRateStr.trim().replace("%", "");
        if ("EXEMPT".equalsIgnoreCase(cleanStr) || "免税".equals(cleanStr)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            BigDecimal rate = new BigDecimal(cleanStr).setScale(2, RoundingMode.HALF_UP);
            validateTaxRate(rate);
            return rate;
        } catch (NumberFormatException e) {
            throw ServiceExceptionUtil.exception(INVOICE_TAX_RATE_INVALID);
        }
    }

    /**
     * 校验税率数值合法性
     */
    public static void validateTaxRate(BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(ONE_HUNDRED) > 0) {
            throw ServiceExceptionUtil.exception(INVOICE_TAX_RATE_INVALID);
        }
    }

    /**
     * 按价税合计总金额与税率反算不含税金额与税额
     *
     * @param totalAmountInclTax 价税合计总金额
     * @param taxRate            税率百分比（如 13.00）
     * @return 计算结果
     */
    public static TaxCalculationResult calculate(BigDecimal totalAmountInclTax, BigDecimal taxRate) {
        validateTaxRate(taxRate);
        BigDecimal inclTax = totalAmountInclTax != null ? totalAmountInclTax.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return TaxCalculationResult.builder()
                    .totalAmountExclTax(inclTax)
                    .totalTax(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalAmountInclTax(inclTax)
                    .taxRate(taxRate.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }

        // 1 + rate / 100
        BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        // totalAmountExclTax = inclTax / divisor, 保留 2 位 HALF_UP
        BigDecimal exclTax = inclTax.divide(divisor, 2, RoundingMode.HALF_UP);
        // totalTax = inclTax - exclTax, 保证两数相加严格等于 inclTax
        BigDecimal tax = inclTax.subtract(exclTax).setScale(2, RoundingMode.HALF_UP);

        return TaxCalculationResult.builder()
                .totalAmountExclTax(exclTax)
                .totalTax(tax)
                .totalAmountInclTax(inclTax)
                .taxRate(taxRate.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}

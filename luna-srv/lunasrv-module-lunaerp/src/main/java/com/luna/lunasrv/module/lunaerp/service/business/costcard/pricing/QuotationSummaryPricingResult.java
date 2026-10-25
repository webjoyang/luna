package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 报价单整单汇总算价结果
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationSummaryPricingResult {

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 总金额（报价币种）
     */
    private BigDecimal totalAmount;

    /**
     * 总成本额（报价币种）
     */
    private BigDecimal totalCostAmount;

    /**
     * 整单加权毛利率（(总金额 − 总成本额) ÷ 总金额；总金额为 0 时为 null）
     */
    private BigDecimal avgMarginRate;

}

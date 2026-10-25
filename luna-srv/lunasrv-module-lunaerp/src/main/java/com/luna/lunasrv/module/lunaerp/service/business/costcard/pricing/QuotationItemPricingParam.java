package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 报价单明细算价入参
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemPricingParam {

    /**
     * 成本单价（人民币）
     */
    private BigDecimal costUnitPriceRmb;

    /**
     * 汇率（1 外币兑换 X 人民币；人民币报价时存 1）
     */
    private BigDecimal exchangeRate;

    /**
     * 目标毛利率（小数，如 0.15 代表 15%）
     */
    private BigDecimal targetMarginRate;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 报价单价（外币/报价币种，可手填；若为空则由目标毛利率反算）
     */
    private BigDecimal unitPrice;

}

package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 报价单明细算价结果
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemPricingResult {

    /**
     * 折报价币成本单价（RMB 成本 ÷ 汇率）
     */
    private BigDecimal costUnitPrice;

    /**
     * 报价单价（报价币种）
     */
    private BigDecimal unitPrice;

    /**
     * 行成本额（数量 × 折币成本）
     */
    private BigDecimal costAmount;

    /**
     * 行金额（数量 × 单价）
     */
    private BigDecimal amount;

    /**
     * 实际毛利率（1 − 成本额 ÷ 金额；金额或成本为 0 时为 null）
     */
    private BigDecimal marginRate;

    /**
     * 数量（原样回传）
     */
    private BigDecimal quantity;

}

package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 成本卡算价结果汇总
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardPricingResult {

    /**
     * 成本基准口径：KG 针织按公斤 / METER 梭织按米
     */
    private String basis;

    /**
     * 每米重量（KG/M）
     */
    private BigDecimal kgPerMeter;

    /**
     * 每公斤米长（M/KG）
     */
    private BigDecimal metersPerKg;

    /**
     * 配比合计（%）
     */
    private BigDecimal ratioTotal;

    /**
     * 缺失价格的纱线数量（price <= 0 或 null）
     */
    private Integer missingPriceCount;

    /**
     * 纱线成本（元/KG）
     */
    private BigDecimal yarnCostPerKg;

    /**
     * 纱线成本（元/M）
     */
    private BigDecimal yarnCostPerMeter;

    /**
     * 费用合计（元/KG）
     */
    private BigDecimal feeTotalPerKg;

    /**
     * 费用合计（元/M）
     */
    private BigDecimal feeTotalPerMeter;

    /**
     * 单位成本不含税（元/KG）
     */
    private BigDecimal unitCostExclTaxPerKg;

    /**
     * 单位成本不含税（元/M）
     */
    private BigDecimal unitCostExclTaxPerMeter;

    /**
     * 单位成本含税（元/KG）
     */
    private BigDecimal unitCostInclTaxPerKg;

    /**
     * 单位成本含税（元/M）
     */
    private BigDecimal unitCostInclTaxPerMeter;

    /**
     * 税率（小数）
     */
    private BigDecimal taxRate;

    /**
     * 各纱线行贡献结果
     */
    @Builder.Default
    private List<YarnRowPricingResult> yarnRowResults = Collections.emptyList();

    /**
     * 各费用行贡献结果
     */
    @Builder.Default
    private List<FeeRowPricingResult> feeRowResults = Collections.emptyList();

}

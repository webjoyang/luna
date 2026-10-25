package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 成本卡算价入参
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardPricingParam {

    /**
     * 织物结构：KNIT 针织 / WOVEN 梭织
     */
    private String fabricConstruction;

    /**
     * 梭织估算方式：SIMPLE 简易估算 / PROFESSIONAL 经纬精算
     */
    private String wovenCalculationMode;

    /**
     * 坯布克重（GSM）
     */
    private BigDecimal greigeWeightGsm;

    /**
     * 坯布门幅（CM）
     */
    private BigDecimal greigeWidthCm;

    /**
     * 税率（小数，例如 0.13 代表 13%，默认 0）
     */
    private BigDecimal taxRate;

    /**
     * 纱线明细行
     */
    @Builder.Default
    private List<YarnPricingRow> yarnRows = Collections.emptyList();

    /**
     * 费用明细行
     */
    @Builder.Default
    private List<FeePricingRow> feeRows = Collections.emptyList();

}

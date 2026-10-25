package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成本卡算价入参 - 纱线明细行
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YarnPricingRow {

    /**
     * 纱线规格 ID
     */
    private Long yarnSpecId;

    /**
     * 纱线规格名称
     */
    private String yarnSpecName;

    /**
     * 单价（元/KG）
     */
    private BigDecimal price;

    /**
     * 配比比例（%）
     */
    private BigDecimal ratio;

    /**
     * 投入损耗率（%）
     */
    private BigDecimal wasteRate;

    /**
     * 方向（GENERAL / WARP / WEFT）
     */
    private String direction;

    /**
     * 密度（经密 / 纬密 / 针织路数）
     */
    private BigDecimal density;

    /**
     * 织缩率（%）
     */
    private BigDecimal crimpRate;

    /**
     * 每米用量（KG/M）
     */
    private BigDecimal consumptionKgPerMeter;

}

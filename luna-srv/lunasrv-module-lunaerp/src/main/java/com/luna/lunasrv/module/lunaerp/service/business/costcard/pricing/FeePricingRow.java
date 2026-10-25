package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成本卡算价入参 - 费用明细行
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePricingRow {

    /**
     * 费用类型（WEAVING / DYEING / SETTING / INSPECTION / PACKAGING / FREIGHT 等）
     */
    private String feeType;

    /**
     * 计价单位（PER_KG / PER_METER）
     */
    private String feeUnit;

    /**
     * 单价（元）
     */
    private BigDecimal price;

    /**
     * 投入损耗率（%）
     */
    private BigDecimal lossRate;

    /**
     * 备注
     */
    private String remark;

}

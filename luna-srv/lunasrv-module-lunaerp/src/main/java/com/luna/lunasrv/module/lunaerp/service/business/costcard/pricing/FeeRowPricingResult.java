package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成本卡算价结果 - 单行费用贡献
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeRowPricingResult {

    /**
     * 费用类型
     */
    private String feeType;

    /**
     * 计价单位（PER_KG / PER_METER）
     */
    private String feeUnit;

    /**
     * 投入损耗放大后的毛额单价
     */
    private BigDecimal grossPrice;

    /**
     * 换算为元/KG 的行贡献（不可算时为 null）
     */
    private BigDecimal contributionPerKg;

    /**
     * 换算为元/M 的行贡献（不可算时为 null）
     */
    private BigDecimal contributionPerMeter;

}

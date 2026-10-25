package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成本卡算价结果 - 单行纱线贡献
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YarnRowPricingResult {

    /**
     * 纱线规格 ID
     */
    private Long yarnSpecId;

    /**
     * 该行在基准口径的贡献金额（针织为元/KG，梭织为元/M）
     */
    private BigDecimal rowContributionInBasis;

    /**
     * 换算为元/KG 的行贡献（不可算时为 null）
     */
    private BigDecimal contributionPerKg;

    /**
     * 换算为元/M 的行贡献（不可算时为 null）
     */
    private BigDecimal contributionPerMeter;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 业务管理 - 成本卡纱线明细行 DO
 *
 * @author luna
 */
@TableName("lunaerp_business_cost_card_yarn")
@KeySequence("lunaerp_business_cost_card_yarn_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardYarnDO extends TenantBaseDO {

    /**
     * 纱线明细行ID
     */
    @TableId
    private Long id;

    /**
     * 所属成本卡ID
     */
    private Long cardId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 纱线材质ID
     */
    private Long yarnTypeId;

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 纱线规格名称快照
     */
    private String yarnSpecName;

    /**
     * 经纬方向（GENERAL / WARP / WEFT）
     */
    private String direction;

    /**
     * 配比百分比（49 = 49%）
     */
    private BigDecimal ratio;

    /**
     * 密度
     */
    private BigDecimal density;

    /**
     * 织缩率%
     */
    private BigDecimal crimpRate;

    /**
     * 投入损耗率%
     */
    private BigDecimal wasteRate;

    /**
     * 每米用量 KG/M
     */
    private BigDecimal consumptionKgPerMeter;

    /**
     * 取价快照 元/KG
     */
    private BigDecimal price;

    /**
     * 取价价类
     */
    private String priceType;

    /**
     * 价格来源（PRICE_TABLE / SPEC_REF_FALLBACK / MANUAL）
     */
    private String priceSource;

    /**
     * 取价生效日期
     */
    private LocalDate priceEffectiveDate;

    /**
     * 行成本贡献 元/KG
     */
    private BigDecimal contributionPerKg;

    /**
     * 行成本贡献 元/M
     */
    private BigDecimal contributionPerMeter;

}

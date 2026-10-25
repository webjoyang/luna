package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 业务管理 - 成本卡主表 DO
 *
 * @author luna
 */
@TableName("lunaerp_business_cost_card")
@KeySequence("lunaerp_business_cost_card_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardDO extends TenantBaseDO {

    /**
     * 成本卡ID
     */
    @TableId
    private Long id;

    /**
     * 成本卡号（CC{yyyy}NNNN，全量唯一含软删行）
     */
    private String cardNo;

    /**
     * 产品档案逻辑关联ID
     */
    private Long productId;

    /**
     * 产品编号快照
     */
    private String productCode;

    /**
     * 品名快照
     */
    private String productName;

    /**
     * 成分快照
     */
    private String composition;

    /**
     * 门幅文本快照
     */
    private String widthSnapshot;

    /**
     * 克重文本快照
     */
    private String weightSnapshot;

    /**
     * 织物结构（KNIT 针织 / WOVEN 梭织）
     */
    private String fabricConstruction;

    /**
     * 梭织模式（SIMPLE 简易 / PROFESSIONAL 经纬精算）
     */
    private String wovenCalculationMode;

    /**
     * 本卡取价价类（PURCHASE 采购参考价 / MARKET 市场参考价）
     */
    private String yarnPriceType;

    /**
     * 坯布克重 g/m²
     */
    private BigDecimal greigeWeightGsm;

    /**
     * 坯布门幅 CM
     */
    private BigDecimal greigeWidthCm;

    /**
     * 每米重量 KG/M（克重×门幅/100000）
     */
    private BigDecimal kgPerMeter;

    /**
     * 税率（如 0.13 表示 13%）
     */
    private BigDecimal taxRate;

    /**
     * 计费基准口径（KG 针织 / METER 梭织）
     */
    private String basis;

    /**
     * 纱线成本 元/KG
     */
    private BigDecimal yarnCostPerKg;

    /**
     * 纱线成本 元/M
     */
    private BigDecimal yarnCostPerMeter;

    /**
     * 费用合计 元/KG
     */
    private BigDecimal totalFeePerKg;

    /**
     * 费用合计 元/M
     */
    private BigDecimal totalFeePerMeter;

    /**
     * 不含税单位成本 元/KG
     */
    private BigDecimal unitCostPerKg;

    /**
     * 含税单位成本 元/KG
     */
    private BigDecimal unitCostPerKgWithTax;

    /**
     * 不含税单位成本 元/M
     */
    private BigDecimal unitCostPerMeter;

    /**
     * 含税单位成本 元/M
     */
    private BigDecimal unitCostPerMeterWithTax;

    /**
     * 缺价纱线行数
     */
    private Integer missingPriceCount;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 状态（DRAFT / EFFECTIVE / EXPIRED）
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;

}

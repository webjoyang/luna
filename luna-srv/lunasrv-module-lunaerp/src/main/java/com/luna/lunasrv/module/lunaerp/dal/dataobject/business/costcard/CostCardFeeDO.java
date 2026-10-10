package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 业务管理 - 成本卡费用明细行 DO
 *
 * @author luna
 */
@TableName("lunaerp_business_cost_card_fee")
@KeySequence("lunaerp_business_cost_card_fee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardFeeDO extends TenantBaseDO {

    /**
     * 费用明细行ID
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
     * 费用类型（WEAVING / DYEING / SETTING / INSPECTION / PACKAGING / FREIGHT）
     */
    private String feeType;

    /**
     * 计费单位（PER_KG / PER_METER）
     */
    private String feeUnit;

    /**
     * 单价，必须 >= 0
     */
    private BigDecimal price;

    /**
     * 投入损耗率%
     */
    private BigDecimal lossRate;

    /**
     * 行费用贡献 元/KG
     */
    private BigDecimal contributionPerKg;

    /**
     * 行费用贡献 元/M
     */
    private BigDecimal contributionPerMeter;

    /**
     * 备注
     */
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 业务管理 - 报价单明细表 DO
 *
 * @author luna
 */
@TableName("lunaerp_business_quotation_item")
@KeySequence("lunaerp_business_quotation_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemDO extends TenantBaseDO {

    /**
     * 报价明细ID
     */
    @TableId
    private Long id;

    /**
     * 所属报价单ID
     */
    private Long quotationId;

    /**
     * 行号（从1递增）
     */
    private Integer lineNo;

    /**
     * 产品档案ID（手工录入时可空）
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
     * 客户色号快照
     */
    private String customerColor;

    /**
     * 成分快照
     */
    private String composition;

    /**
     * 门幅快照
     */
    private String width;

    /**
     * 克重快照
     */
    private String weight;

    /**
     * 单位（KG / M / Y，缺省 KG）
     */
    private String unit;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 来源成本卡ID（手工输入时为空）
     */
    private Long costCardId;

    /**
     * 来源成本卡号快照
     */
    private String costCardNo;

    /**
     * 成本来源（CARD / MANUAL）
     */
    private String costSource;

    /**
     * 成本卡整份快照（JSON，卡号/日期/成本口径/明细）
     */
    private String costSnapshot;

    /**
     * RMB 单位成本（脱敏字段）
     */
    private BigDecimal costUnitPriceRmb;

    /**
     * 汇率快照（报价币对人民币）
     */
    private BigDecimal exchangeRateSnapshot;

    /**
     * 折报价币单位成本（costUnitPriceRmb / exchangeRateSnapshot，脱敏字段）
     */
    private BigDecimal costUnitPrice;

    /**
     * 目标毛利率（反算单价输入，脱敏字段）
     */
    private BigDecimal targetMarginRate;

    /**
     * 报价单价（报价币种）
     */
    private BigDecimal unitPrice;

    /**
     * 行成本额（quantity * costUnitPrice，脱敏字段）
     */
    private BigDecimal costAmount;

    /**
     * 行金额（quantity * unitPrice）
     */
    private BigDecimal amount;

    /**
     * 实际毛利率（(amount - costAmount) / amount，脱敏字段）
     */
    private BigDecimal marginRate;

    /**
     * 低毛利预警（marginRate < minMarginRate，布尔值不脱敏）
     */
    private Boolean lowMarginWarning;

    /**
     * 行备注
     */
    private String remark;

}

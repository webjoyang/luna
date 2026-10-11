package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 供应商对账单行表 DO
 *
 * 表：lunaerp_finance_supplier_recon_item
 *
 * 行类型：PROCESS 加工费行 / PURCHASE 采购货款行 / ADJUST 调整项行；
 * 快照表设计，生成时从源单复制，零回写源单；确认后整表冻结。
 *
 * @author luna
 */
@TableName("lunaerp_finance_supplier_recon_item")
@KeySequence("lunaerp_finance_supplier_recon_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconItemDO extends TenantBaseDO {

    /**
     * 对账单行ID
     */
    private Long id;

    /**
     * 关联对账单主表ID（lunaerp_finance_supplier_recon.id）
     */
    private Long reconId;

    /**
     * 行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）
     */
    private String lineType;

    /**
     * 源单据类型（INSPECTION / STOCK_IN_ITEM，ADJUST 为空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourceRefType;

    /**
     * 源单据主表ID（检验单ID / 入库单ID）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long sourceRefId;

    /**
     * 源单据明细ID（入库明细ID，检验单为汇总行时为空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long sourceRefItemId;

    /**
     * 检验单ID快照（PROCESS 行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionOrderId;

    /**
     * 检验单号快照（PROCESS 行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionOrderNo;

    /**
     * 检验类型快照（GREIGE 胚布检验 / FINISHED 成品检验）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionType;

    /**
     * 入库单ID快照（PURCHASE 行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stockInId;

    /**
     * 入库单号快照（PURCHASE 行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String stockInNo;

    /**
     * 关联纱线采购单ID快照（PURCHASE 行，可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long yarnOrderId;

    /**
     * 关联纱线采购单号快照（PURCHASE 行，可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String yarnOrderNo;

    /**
     * 产品ID快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long productId;

    /**
     * 产品编号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;

    /**
     * 产品名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productName;

    /**
     * 批次/缸号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String batchNo;

    /**
     * 重量数量（KG，PROCESS：实称总重；PURCHASE：KG 单位量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal qtyKg;

    /**
     * 米数数量（M，PROCESS：实测总米数；PURCHASE：M 单位量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal qtyM;

    /**
     * 计价基准（KG / METER）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String pricingBasis;

    /**
     * 结算计费数量（基准对应量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal billQty;

    /**
     * 结算单价（元/KG 或 元/M）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal unitPrice;

    /**
     * 行金额（元，bill_qty * unit_price，调整项可正可负）
     */
    private BigDecimal lineAmount;

    /**
     * 调整原因（ADJUST 行必填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String adjustReason;

    /**
     * 行备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

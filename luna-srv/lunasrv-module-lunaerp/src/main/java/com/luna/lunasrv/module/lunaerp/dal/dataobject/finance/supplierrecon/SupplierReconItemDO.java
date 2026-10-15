package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务管理 - 供应商对账单行表 DO（快照）
 *
 * 表：lunaerp_finance_supplier_recon_item
 *
 * 三类行：PROCESS 加工费行 / PURCHASE 采购货款行 / ADJUST 调整项行；
 * 行表即快照，生成时从源单复制，零回写；确认后行数据冻结；
 * 防重复取数靠 source_ref 指纹（DRAFT/CONFIRMED 占用，CANCELLED 释放）；
 * 金额 numeric(18,2)、单价 numeric(18,4)、数量 numeric(18,2)。
 *
 * @author luna
 */
@TableName("lunaerp_finance_supplier_recon_item")
@KeySequence("lunaerp_finance_supplier_recon_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierReconItemDO extends TenantBaseDO {

    /**
     * 对账单明细行ID
     */
    private Long id;

    /**
     * 所属对账单ID（lunaerp_finance_supplier_recon.id）
     */
    private Long reconId;

    /**
     * 行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）
     */
    private String lineType;

    /**
     * 来源单据类型（INSPECTION 检验单 / STOCK_IN_ITEM 入库明细；ADJUST 为空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourceRefType;

    /**
     * 来源单据主表ID（检验单ID / 入库单ID）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long sourceRefId;

    /**
     * 来源单据明细ID（入库明细ID，检验单为汇总行可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long sourceRefItemId;

    /**
     * 加工源检验单ID（PROCESS 必填快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionOrderId;

    /**
     * 加工源检验单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionOrderNo;

    /**
     * 检验单类型快照（GREIGE 胚布检验=织费 / FINISHED 成品检验=染费）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionType;

    /**
     * 采购源入库单ID（PURCHASE 必填快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stockInId;

    /**
     * 采购源入库单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String stockInNo;

    /**
     * 采购源入库单明细ID（PURCHASE 必填快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stockInItemId;

    /**
     * 纱线采购单ID快照（可选，入库单带出）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long yarnOrderId;

    /**
     * 纱线采购单号快照（追溯用，可选）
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
     * 千克数量（PROCESS 对应实称重量合计，PURCHASE KG 单位数量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal qtyKg;

    /**
     * 米数数量（PROCESS 对应检验米数合计，PURCHASE M 单位数量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal qtyM;

    /**
     * 数量单位（KG / M）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String qtyUnit;

    /**
     * 计价基准（KG 按千克 / METER 按米；GREIGE 缺省米，FINISHED 缺省公斤）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String pricingBasis;

    /**
     * 结算计费数量（基准对应量，元数据/重算权威）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal billQty;

    /**
     * 结算单价（元，手工填/入库明细带出可改）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal unitPrice;

    /**
     * 行金额（元，line_amount = bill_qty × unit_price 服务端重算；ADJUST 直接填正负数）
     */
    private BigDecimal lineAmount;

    /**
     * 调整原因（ADJUST 行必填，如质量扣款、运费补差、退货冲减）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String adjustReason;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /**
     * 卷数（内存/展示辅助字段，不落库）
     */
    @TableField(exist = false)
    private Integer rollCount;

}

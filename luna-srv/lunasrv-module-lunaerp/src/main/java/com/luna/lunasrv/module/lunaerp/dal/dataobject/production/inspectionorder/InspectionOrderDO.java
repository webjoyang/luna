package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理-检验单主表 DO
 *
 * 表：lunaerp_production_inspection_order
 *
 * 检验单号 INS-YYYYMMDD-NNN 按天取号、租户内全量唯一、软删不释放（ADR-0015）。
 * 录检明细作为独立子资源处理（ADR-0014）。
 *
 * @author luna
 */
@TableName("lunaerp_production_inspection_order")
@KeySequence("lunaerp_production_inspection_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionOrderDO extends TenantBaseDO {

    /**
     * 检验单ID
     */
    private Long id;

    /**
     * 检验单号（INS-YYYYMMDD-NNN，按天取号，租户内全量唯一含软删行，ADR-0015）
     */
    private String code;

    /**
     * 检验类型（GREIGE 胚布检验 / FINISHED 成品检验）
     */
    private String type;

    /**
     * 检验单状态（字典 lunaerp_inspection_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED），创建缺省 DRAFT
     */
    private String status;

    /**
     * 关联产品ID（lunaerp_baseinfo_product.id，必填）
     */
    private Long productId;

    /**
     * 产品编码快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;

    /**
     * 产品名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productName;

    /**
     * 关联胚布单ID（lunaerp_production_greige_order.id，type=GREIGE 时可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long greigeOrderId;

    /**
     * 关联染色单ID（lunaerp_production_dyeing_order.id，type=FINISHED 时可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dyeingOrderId;

    /**
     * 承检方供应商ID（lunaerp_baseinfo_supplier.id，可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionFactoryId;

    /**
     * 承检方公司名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionFactoryName;

    /**
     * 关联销售合同ID（lunaerp_business_sales_contract.id，可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salesContractId;

    /**
     * 合同号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;

    /**
     * 目标重量 KG
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal targetWeight;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /**
     * 单据日期（必填，创建缺省当天）
     */
    private LocalDate orderDate;

}

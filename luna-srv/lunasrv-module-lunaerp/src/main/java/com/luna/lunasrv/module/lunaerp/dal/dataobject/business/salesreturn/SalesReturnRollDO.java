package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务管理 - 销售退货卷明细 DO（ADR-0042）
 *
 * 表：lunaerp_business_sales_return_roll
 *
 * @author luna
 */
@TableName("lunaerp_business_sales_return_roll")
@KeySequence("lunaerp_business_sales_return_roll_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesReturnRollDO extends TenantBaseDO {

    /**
     * 退货卷ID
     */
    private Long id;

    /**
     * 所属退货单ID（lunaerp_business_sales_return.id）
     */
    private Long returnId;

    /**
     * 所属退货明细ID（lunaerp_business_sales_return_item.id）
     */
    private Long returnItemId;

    /**
     * 关联原发货卷ID（lunaerp_business_shipment_roll.id，可选，散退为空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long shipmentRollId;

    /**
     * 卷号（勾选时带回可改，手填必填）
     */
    private String rollNo;

    /**
     * 实称重量（KG）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal weight;

    /**
     * 米数（M）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal lengthM;

    /**
     * 质检结果判定（字典 lunaerp_sales_return_qc_result：RESTOCK/SCRAP）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String qcResult;

    /**
     * 质检判定人ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long qcBy;

    /**
     * 质检判定时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime qcAt;

    /**
     * 质检处置备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String qcRemark;

    /**
     * 目标库位ID（仅 RESTOCK 卷，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long locationId;

    /**
     * 目标库位编码快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String locationCode;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

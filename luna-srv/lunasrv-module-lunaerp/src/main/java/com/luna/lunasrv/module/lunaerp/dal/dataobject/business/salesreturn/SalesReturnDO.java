package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业务管理 - 销售退货主表 DO（ADR-0042）
 *
 * 表：lunaerp_business_sales_return
 *
 * @author luna
 */
@TableName("lunaerp_business_sales_return")
@KeySequence("lunaerp_business_sales_return_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesReturnDO extends TenantBaseDO {

    /**
     * 退货单ID
     */
    private Long id;

    /**
     * 退货单号（RT-YYYYMMDD-NNN，租户内全量唯一含软删行）
     */
    private String returnNo;

    /**
     * 业务状态（字典 lunaerp_sales_return_status：DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）
     */
    private String status;

    /**
     * 审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）
     */
    private String approvalStatus;

    /**
     * 发货单ID（lunaerp_business_shipment.id，必选 COMPLETED 发货单）
     */
    private Long shipmentId;

    /**
     * 发货单号快照
     */
    private String shipmentNo;

    /**
     * 客户ID（lunaerp_baseinfo_customer.id，头单快照）
     */
    private Long customerId;

    /**
     * 客户名称快照
     */
    private String customerName;

    /**
     * 销售合同ID（lunaerp_business_sales_contract.id，头单快照，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salesContractId;

    /**
     * 销售合同号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;

    /**
     * 退货仓库ID（lunaerp_stock_warehouse.id，缺省=原发货仓库）
     */
    private Long warehouseId;

    /**
     * 退货仓库名称快照
     */
    private String warehouseName;

    /**
     * 币种（RMB/USD/EUR/HKD，头单快照）
     */
    private String currency;

    /**
     * 退货总金额（元，服务端重算 Σ行金额）
     */
    private BigDecimal totalAmount;

    /**
     * 退货总匹数（服务端重算 Σ行匹数）
     */
    private Integer totalPieces;

    /**
     * 入库单ID（lunaerp_stock_in.id，审批通过回写）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stockInId;

    /**
     * 到货质检确认人ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long arrivedBy;

    /**
     * 到货质检确认时间（RECEIVED 状态落）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime arrivedAt;

    /**
     * 财务过账确认人ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long postedBy;

    /**
     * 财务过账确认时间（POSTED 状态落）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime postedAt;

    /**
     * 作废人ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long cancelledBy;

    /**
     * 作废时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime cancelledAt;

    /**
     * 作废原因
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cancelReason;

    /**
     * 退货单据日期（缺省当天）
     */
    private LocalDate returnDate;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

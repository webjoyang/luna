package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment;

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
 * 业务管理 - 发货单主表 DO（ADR-0029）
 *
 * 表：lunaerp_business_shipment
 *
 * @author luna
 */
@TableName("lunaerp_business_shipment")
@KeySequence("lunaerp_business_shipment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ShipmentDO extends TenantBaseDO {

    /**
     * 发货单ID
     */
    private Long id;
    /**
     * 发货单号（SH-YYYYMMDD-NNN，租户内全量唯一含软删行）
     */
    private String shipmentNo;
    /**
     * 发货状态（字典 lunaerp_shipment_status：DRAFT/COMPLETED/CANCELLED）
     */
    private String status;
    /**
     * 客户ID（lunaerp_baseinfo_customer.id，必填）
     */
    private Long customerId;
    /**
     * 客户名称快照
     */
    private String customerName;
    /**
     * 销售合同ID（lunaerp_business_sales_contract.id，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salesContractId;
    /**
     * 销售合同号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;
    /**
     * 发货仓库ID（lunaerp_stock_warehouse.id，必填）
     */
    private Long warehouseId;
    /**
     * 发货仓库名称快照
     */
    private String warehouseName;
    /**
     * 发货地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String shippingAddress;
    /**
     * 收货地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String receiveAddress;
    /**
     * 承运商/物流公司（自由文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String carrier;
    /**
     * 物流单号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String trackingNumber;
    /**
     * 发货日期
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate shippingDate;
    /**
     * 预计到货日期
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate expectedDeliveryDate;
    /**
     * 运费（独立于货物金额，缺省 0）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal freightFee;
    /**
     * 货物金额合计（Σ明细金额，服务端重算）
     */
    private BigDecimal totalAmount;
    /**
     * 总匹数（Σ明细匹数，服务端重算）
     */
    private Integer totalPieces;
    /**
     * 业务员用户ID（system_users.id，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salespersonUserId;
    /**
     * 归属部门（数据范围 dept 列）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deptId;
    /**
     * 业务员姓名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String salespersonName;
    /**
     * 币种（复用字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD），缺省 RMB
     */
    private String currency;
    /**
     * 签收状态（字典 lunaerp_shipment_sign_status：PENDING/SIGNED/REFUSED），缺省 PENDING
     */
    private String signStatus;
    /**
     * 签收人姓名
     */
    private String signedByName;
    /**
     * 签收时间
     */
    private LocalDateTime signedAt;
    /**
     * 签收备注
     */
    private String signRemark;
    /**
     * 发货备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

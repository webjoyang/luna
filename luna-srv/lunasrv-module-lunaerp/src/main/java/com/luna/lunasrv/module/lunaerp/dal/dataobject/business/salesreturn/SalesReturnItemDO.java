package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务管理 - 销售退货明细行 DO（ADR-0042）
 *
 * 表：lunaerp_business_sales_return_item
 *
 * @author luna
 */
@TableName("lunaerp_business_sales_return_item")
@KeySequence("lunaerp_business_sales_return_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesReturnItemDO extends TenantBaseDO {

    /**
     * 退货明细ID
     */
    private Long id;

    /**
     * 所属退货单ID（lunaerp_business_sales_return.id）
     */
    private Long returnId;

    /**
     * 强制发货明细血缘ID（lunaerp_business_shipment_item.id）
     */
    private Long shipmentItemId;

    /**
     * 产品ID（lunaerp_baseinfo_product.id）
     */
    private Long productId;

    /**
     * 产品编号快照
     */
    private String productCode;

    /**
     * 产品名称快照
     */
    private String productName;

    /**
     * 客户色号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String customerColorNo;

    /**
     * 颜色名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorName;

    /**
     * 缸号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String vatNo;

    /**
     * 退货计费数量（手填，防超退校验）
     */
    private BigDecimal returnQty;

    /**
     * 匹数（缺省 1）
     */
    private Integer pieces;

    /**
     * 单位（KG/M，从发货行快照）
     */
    private String unit;

    /**
     * 单价（元，缺省=原发货单价，允许改折让价）
     */
    private BigDecimal unitPrice;

    /**
     * 行金额（元，服务端重算 = returnQty * unitPrice）
     */
    private BigDecimal amount;

    /**
     * 退货原因（字典 lunaerp_sales_return_reason：QUALITY/WRONG/EXCESS/CUSTOMER/OTHER）
     */
    private String returnReason;

    /**
     * 可二次入库数量（到货质检时服务端按 RESTOCK 卷汇总）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal restockQty;

    /**
     * 废品数量（到货质检时服务端按 SCRAP 卷汇总）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal scrapQty;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

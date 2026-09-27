package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务管理 - 发货明细表 DO（ADR-0003, ADR-0029）
 *
 * 表：lunaerp_business_shipment_item
 *
 * 明细是发货单聚合的一部分：无独立 Controller/权限码，随整单保存整体替换；
 * 删除发货单时按 shipment_id 级联逻辑删除。
 *
 * @author luna
 */
@TableName("lunaerp_business_shipment_item")
@KeySequence("lunaerp_business_shipment_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ShipmentItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;
    /**
     * 所属发货单ID（lunaerp_business_shipment.id，必填）
     */
    private Long shipmentId;
    /**
     * 产品ID（lunaerp_baseinfo_product.id，必填）
     */
    private Long productId;
    /**
     * 产品编码快照
     */
    private String productCode;
    /**
     * 产品名称快照
     */
    private String productName;
    /**
     * 客户色号
     */
    private String customerColorNo;
    /**
     * 颜色名称
     */
    private String colorName;
    /**
     * 缸号/批次号（自由文本）
     */
    private String vatNo;
    /**
     * 发货数量（>0，必填）
     */
    private BigDecimal quantity;
    /**
     * 匹数（缺省 1）
     */
    private Integer pieces;
    /**
     * 单位（复用字典 lunaerp_stock_in_unit：KG/M），缺省 KG
     */
    private String unit;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 行金额（服务端重算：数量×单价）
     */
    private BigDecimal amount;
    /**
     * 来源入库明细ID（lunaerp_stock_in_item.id 快照，追溯用，零回写）
     */
    private Long sourceStockInItemId;
    /**
     * 明细备注
     */
    private String remark;

}

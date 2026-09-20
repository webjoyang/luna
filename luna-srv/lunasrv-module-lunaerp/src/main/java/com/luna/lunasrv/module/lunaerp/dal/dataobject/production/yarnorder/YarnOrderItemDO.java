package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-纱线采购明细表 DO
 *
 * 表：lunaerp_production_yarn_order_item
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_order_item")
@KeySequence("lunaerp_production_yarn_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnOrderItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    @TableId
    private Long id;

    /**
     * 所属采购单 ID
     */
    private Long orderId;

    /**
     * 纱线材质 ID
     */
    private Long yarnTypeId;

    /**
     * 纱线规格 ID
     */
    private Long yarnSpecId;

    /**
     * 供应商 ID
     */
    private Long supplierId;

    /**
     * 供应商公司名快照
     */
    private String supplierName;

    /**
     * 配比百分比数字快照（49 = 49%）
     */
    private BigDecimal ratio;

    /**
     * 损耗率百分比数字快照
     */
    private BigDecimal lossRate;

    /**
     * 基础需求量
     */
    private BigDecimal baseQty;

    /**
     * 含损耗需求量 = base_qty × (1 + loss_rate/100)
     */
    private BigDecimal requiredQty;

    /**
     * 在库量快照
     */
    private BigDecimal stockQty;

    /**
     * 实采量
     */
    private BigDecimal purchaseQty;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额 = unit_price × purchase_qty
     */
    private BigDecimal totalAmount;

    /**
     * 历史价回填
     */
    private BigDecimal refUnitPrice;

    /**
     * 备注
     */
    private String remark;

}

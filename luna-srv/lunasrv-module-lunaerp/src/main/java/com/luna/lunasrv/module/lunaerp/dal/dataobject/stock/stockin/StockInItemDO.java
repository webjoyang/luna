package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存管理 - 入库明细表 DO（ADR-0003 聚合保存模式）
 *
 * 表：lunaerp_stock_in_item
 *
 * @author luna
 */
@TableName("lunaerp_stock_in_item")
@KeySequence("lunaerp_stock_in_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;
    /**
     * 所属入库单ID
     */
    private Long stockInId;
    /**
     * 关联产品ID
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
     * 批次号/缸号
     */
    private String batchNo;
    /**
     * 卷号
     */
    private String rollNo;
    /**
     * 匹数（缺省 1）
     */
    private Integer pieceCount;
    /**
     * 数量（必填，>0）
     */
    private BigDecimal quantity;
    /**
     * 单位（字典 lunaerp_stock_in_unit：KG/M）
     */
    private String unit;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 金额（quantity * unit_price，服务端重算）
     */
    private BigDecimal amount;
    /**
     * 备注
     */
    private String remark;

}

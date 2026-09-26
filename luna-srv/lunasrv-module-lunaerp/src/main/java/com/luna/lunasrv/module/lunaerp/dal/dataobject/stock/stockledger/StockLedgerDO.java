package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存管理 - 库存流水台账 DO（ADR-0017）
 *
 * 表：lunaerp_stock_ledger
 *
 * 通用产品维度流水台账，只存流水不存余额；只增不改不删；
 * txn_type: STOCK_IN（入库正数）/ STOCK_IN_REVERSAL（红冲负数），内部枚举不入字典；
 * 余额由 SUM(quantity) GROUP BY product_id, unit, warehouse_id 实时聚合。
 *
 * @author luna
 */
@TableName("lunaerp_stock_ledger")
@KeySequence("lunaerp_stock_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockLedgerDO extends TenantBaseDO {

    /**
     * 流水ID
     */
    private Long id;
    /**
     * 物品类型（PRODUCT / YARN_SPEC），缺省 PRODUCT
     */
    private String itemType;
    /**
     * 关联产品ID（lunaerp_baseinfo_product.id，物品为 PRODUCT 时必填，与 yarnSpecId 互斥）
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
     * 关联纱线规格ID（lunaerp_production_yarn_spec.id，物品为 YARN_SPEC 时必填，与 productId 互斥）
     */
    private Long yarnSpecId;
    /**
     * 纱线规格名称快照
     */
    private String yarnSpecName;
    /**
     * 仓库ID（lunaerp_stock_warehouse.id，必填）
     */
    private Long warehouseId;
    /**
     * 仓库名称快照
     */
    private String warehouseName;
    /**
     * 库位ID（lunaerp_stock_warehouse_location.id，可空）
     */
    private Long locationId;
    /**
     * 库位编码快照
     */
    private String locationCode;
    /**
     * 批次/缸号
     */
    private String batchNo;
    /**
     * 交易类型（STOCK_IN 入库 / STOCK_IN_REVERSAL 红冲），内部枚举不入字典
     */
    private String txnType;
    /**
     * 带符号数量（入库正、红冲负）
     */
    private BigDecimal quantity;
    /**
     * 单位（KG/M）
     */
    private String unit;
    /**
     * 单价快照
     */
    private BigDecimal unitPrice;
    /**
     * 金额快照（带符号，红冲负）
     */
    private BigDecimal amount;
    /**
     * 关联单据类型（本期固定 STOCK_IN）
     */
    private String relatedDocType;
    /**
     * 关联入库单ID（lunaerp_stock_in.id）
     */
    private Long relatedDocId;
    /**
     * 关联入库单号快照
     */
    private String relatedDocNo;
    /**
     * 备注
     */
    private String remark;

}

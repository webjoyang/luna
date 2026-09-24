package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存管理 - 库位分配放置记录表 DO（ADR-0018）
 *
 * 表：lunaerp_stock_in_item_placement
 *
 * @author luna
 */
@TableName("lunaerp_stock_in_item_placement")
@KeySequence("lunaerp_stock_in_item_placement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInItemPlacementDO extends TenantBaseDO {

    /**
     * 放置记录ID
     */
    private Long id;
    /**
     * 所属入库单ID
     */
    private Long stockInId;
    /**
     * 所属入库明细ID
     */
    private Long stockInItemId;
    /**
     * 库位ID
     */
    private Long locationId;
    /**
     * 库位编码快照
     */
    private String locationCode;
    /**
     * 放置匹数
     */
    private Integer pieceCount;
    /**
     * 放置数量
     */
    private BigDecimal quantity;
    /**
     * 单位（KG/M）
     */
    private String unit;
    /**
     * 备注
     */
    private String remark;

}

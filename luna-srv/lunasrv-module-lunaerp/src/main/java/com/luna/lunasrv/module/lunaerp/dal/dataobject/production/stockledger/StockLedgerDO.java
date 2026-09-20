package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.stockledger;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-纱线库存通用流水台账 DO
 *
 * 表：lunaerp_production_stock_ledger
 * 通用流水台账（去 source_location/target_location 两列，ADR-0012）。
 *
 * @author luna
 */
@TableName("lunaerp_production_stock_ledger")
@KeySequence("lunaerp_production_stock_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockLedgerDO extends TenantBaseDO {

    /**
     * 流水ID
     */
    private Long id;
    /**
     * 纱线规格ID（lunaerp_production_yarn_spec.id）
     */
    private Long yarnSpecId;
    /**
     * 流水类型（STOCK_IN 入库 / STOCK_OUT 出库 / PROCESS_IN 生产入库 / PROCESS_OUT 生产出库，MVP仅用STOCK_IN）
     */
    private String type;
    /**
     * 变动数量（STOCK_IN正/STOCK_OUT负）
     */
    private BigDecimal quantity;
    /**
     * 关联单据类型（如 YARN_ORDER）
     */
    private String relatedDocType;
    /**
     * 关联单据ID
     */
    private Long relatedDocId;
    /**
     * 批号
     */
    private String batchNo;
    /**
     * 备注
     */
    private String remark;

}

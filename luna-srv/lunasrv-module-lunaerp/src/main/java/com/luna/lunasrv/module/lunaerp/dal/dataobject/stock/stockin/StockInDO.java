package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存管理 - 入库单主表 DO（ADR-0015, ADR-0016）
 *
 * 表：lunaerp_stock_in
 *
 * @author luna
 */
@TableName("lunaerp_stock_in")
@KeySequence("lunaerp_stock_in_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInDO extends TenantBaseDO {

    /**
     * 入库单ID
     */
    private Long id;
    /**
     * 入库单号（IN-YYYYMMDD-NNN，租户内全量唯一含软删行）
     */
    private String inNo;
    /**
     * 入库类型（字典 lunaerp_stock_in_type：PURCHASE/PRODUCTION/RETURN/OTHER）
     */
    private String inType;
    /**
     * 状态（字典 lunaerp_stock_in_status：DRAFT/COMPLETED/CANCELLED）
     */
    private String status;
    /**
     * 入库仓库ID
     */
    private Long warehouseId;
    /**
     * 仓库名称快照
     */
    private String warehouseName;
    /**
     * 供应商ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;
    /**
     * 供应商名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String supplierName;
    /**
     * 关联销售合同ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salesContractId;
    /**
     * 关联合同号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;
    /**
     * 关联胚布指示单ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long greigeOrderId;
    /**
     * 关联胚布指示单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String greigeOrderNo;
    /**
     * 关联染色指示单ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dyeingOrderId;
    /**
     * 关联胚布品种ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long greigeTypeId;
    /**
     * 关联胚布品种名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String greigeTypeName;
    /**
     * 关联检验单ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionOrderId;
    /**
     * 关联检验单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionOrderNo;
    /**
     * 金额合计（服务端重算，明细金额之和）
     */
    private BigDecimal totalAmount;
    /**
     * 入库单据日期
     */
    private LocalDate inDate;
    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

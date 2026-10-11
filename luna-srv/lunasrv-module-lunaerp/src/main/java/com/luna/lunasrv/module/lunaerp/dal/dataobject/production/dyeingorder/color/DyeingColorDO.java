package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-染色单颜色明细子表 DO
 *
 * 表：lunaerp_production_dyeing_color
 *
 * 随染色单聚合保存（ADR-0003）。
 *
 * @author luna
 */
@TableName("lunaerp_production_dyeing_color")
@KeySequence("lunaerp_production_dyeing_color_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DyeingColorDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;
    /**
     * 染色单ID（lunaerp_production_dyeing_order.id）
     */
    private Long dyeingOrderId;
    /**
     * 行号（从 1 开始）
     */
    private Integer lineNo;
    /**
     * 客户色号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String customerColorNo;
    /**
     * 颜色名称
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorName;
    /**
     * 本单染色数量（成品口径）
     */
    private BigDecimal quantity;
    /**
     * 匹数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pieces;
    /**
     * 投染数（含损耗的实际投入量）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal dyeingInputQty;
    /**
     * 投染数单位（字典 lunaerp_sales_contract_unit：KG/M）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dyeingInputUnit;
    /**
     * 合同数量（从合同明细带出，只读对比）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal contractQty;
    /**
     * 来源合同行 ID（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long contractLineId;
    /**
     * 来源合同行号快照（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer contractLineNo;

}

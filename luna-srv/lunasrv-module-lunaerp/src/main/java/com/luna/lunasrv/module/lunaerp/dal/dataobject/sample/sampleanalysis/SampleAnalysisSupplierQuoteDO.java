package com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 样品管理-样品分析供应商报价子表 DO
 *
 * 表：lunaerp_sample_analysis_supplier_quote
 *
 * @author luna
 */
@TableName("lunaerp_sample_analysis_supplier_quote")
@KeySequence("lunaerp_sample_analysis_supplier_quote_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleAnalysisSupplierQuoteDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 样品分析ID（lunaerp_sample_analysis.id）
     */
    private Long sampleAnalysisId;

    /**
     * 供应商ID（lunaerp_baseinfo_supplier.id，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    /**
     * 供应商名称快照（手填或联想带出）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String supplierName;

    /**
     * 胚布报价（元/KG，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal greigePrice;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

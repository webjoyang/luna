package com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 样品管理 - 样品分析纱线规格明细子表 DO
 *
 * 表：lunaerp_sample_analysis_yarn_spec
 * 随样品分析主表聚合差异保存（ADR-0032、ADR-0036）。
 *
 * @author luna
 */
@TableName("lunaerp_sample_analysis_yarn_spec")
@KeySequence("lunaerp_sample_analysis_yarn_spec_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleAnalysisYarnSpecDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;
    /**
     * 样品分析ID（lunaerp_sample_analysis.id）
     */
    private Long sampleAnalysisId;
    /**
     * 纱线规格ID（lunaerp_production_yarn_spec.id）
     */
    private Long yarnSpecId;
    /**
     * 纱线材质ID快照（lunaerp_production_yarn_type.id）
     */
    private Long yarnTypeId;
    /**
     * 纱线材质名称快照
     */
    private String yarnTypeName;
    /**
     * 纱线规格名称快照
     */
    private String specName;
    /**
     * 纺法快照
     */
    private String spinMethod;
    /**
     * 用纱比例（百分比数字，如 30 表示 30%）
     */
    private BigDecimal ratio;
    /**
     * 单价（元/KG）
     */
    private BigDecimal unitPrice;
    /**
     * 备注
     */
    private String remark;

}

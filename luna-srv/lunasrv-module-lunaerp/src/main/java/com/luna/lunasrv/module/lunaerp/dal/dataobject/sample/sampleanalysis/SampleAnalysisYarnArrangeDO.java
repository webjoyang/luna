package com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样品管理-样品分析绕纱行子表 DO
 *
 * 表：lunaerp_sample_analysis_yarn_arrange
 * 避让 SQL 保留词 desc，描述列命名为 description（见 ADR-0036）
 *
 * @author luna
 */
@TableName("lunaerp_sample_analysis_yarn_arrange")
@KeySequence("lunaerp_sample_analysis_yarn_arrange_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleAnalysisYarnArrangeDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 所属样品分析ID
     */
    private Long sampleAnalysisId;

    /**
     * 路号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String code;

    /**
     * 绕纱表述（如 25.1cm/1 花宽）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftText;

    /**
     * 对针（如 第一路对针）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String borderStatus;

    /**
     * 描述（如 开幅线）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-纱线规格子表 DO
 *
 * 表：lunaerp_production_yarn_spec
 * 挂在材质（lunaerp_production_yarn_type）之下，采购明细行直接关联的实体。
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_spec")
@KeySequence("lunaerp_production_yarn_spec_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnSpecDO extends TenantBaseDO {

    /**
     * 规格ID
     */
    private Long id;
    /**
     * 所属材质ID（lunaerp_production_yarn_type.id）
     */
    private Long yarnTypeId;
    /**
     * 规格名称（展示用，如"32S 棉"）
     */
    private String specName;
    /**
     * 标准名称（租户内+材质内唯一）
     */
    private String standardName;
    /**
     * 纺纱工艺（环锭纺/转杯纺/喷气纺等）
     */
    private String spinMethod;
    /**
     * 光泽（半光/全光/消光等）
     */
    private String luster;
    /**
     * 参考单价（测算历史价回填兜底，ADR-0011）
     */
    private BigDecimal refPrice;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE
     */
    private String status;

}

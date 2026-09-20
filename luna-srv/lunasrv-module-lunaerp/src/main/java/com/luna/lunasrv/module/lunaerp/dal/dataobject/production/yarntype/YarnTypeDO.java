package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产管理-纱线材质主表 DO
 *
 * 表：lunaerp_production_yarn_type
 * 材质类型不入字典（ADR-0011），直接由 seed 表与该表维护。
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_type")
@KeySequence("lunaerp_production_yarn_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnTypeDO extends TenantBaseDO {

    /**
     * 材质ID
     */
    private Long id;
    /**
     * 材质名称（棉/涤/人棉/氨纶/锦纶/粘胶/混纺，租户内唯一）
     */
    private String name;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE
     */
    private String status;

}

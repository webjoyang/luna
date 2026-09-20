package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产管理-纱线配比BOM主表 DO
 *
 * 表：lunaerp_production_yarn_recipe_bom
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_recipe_bom")
@KeySequence("lunaerp_production_yarn_recipe_bom_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRecipeBomDO extends TenantBaseDO {

    /**
     * 配比BOM ID
     */
    @TableId
    private Long id;

    /**
     * 产品档案ID（强关联产品档案，租户内一产品一BOM）
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
     * 备注
     */
    private String remark;

    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用）
     */
    private String status;

}

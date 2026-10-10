package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-纱线配比BOM明细表 DO
 *
 * 表：lunaerp_production_yarn_recipe_bom_item
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_recipe_bom_item")
@KeySequence("lunaerp_production_yarn_recipe_bom_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRecipeBomItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    @TableId
    private Long id;

    /**
     * 所属配比BOM ID
     */
    private Long recipeBomId;

    /**
     * 纱线材质ID
     */
    private Long yarnTypeId;

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 配比百分比数字（49 = 49%）
     */
    private BigDecimal ratio;

    /**
     * 纺纱工艺（可空）
     */
    private String spinMethod;

    /**
     * 备注
     */
    private String remark;

    /**
     * 参考单价（可空，测算兜底取规格 ref_price）
     */
    private BigDecimal refPrice;

    /**
     * 损耗率（百分比数字，如 2.50 表示 2.5%）
     */
    private BigDecimal wasteRate;

    /**
     * 方向（GENERAL 通用 / WARP 经纱 / WEFT 纬纱）
     */
    private String direction;

    /**
     * 密度（经密 / 纬密 / 针织路数）
     */
    private BigDecimal density;

    /**
     * 织缩率（百分比数字，如 5.00 表示 5%）
     */
    private BigDecimal crimpRate;

    /**
     * 每米用量（KG/米）
     */
    private BigDecimal consumptionKgPerMeter;

}

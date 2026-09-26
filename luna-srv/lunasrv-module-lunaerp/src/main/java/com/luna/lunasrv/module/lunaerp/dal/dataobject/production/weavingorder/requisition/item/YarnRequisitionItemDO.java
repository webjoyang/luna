package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理 - 领纱明细表 DO（独立子资源）
 *
 * 表：lunaerp_production_yarn_requisition_item
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_requisition_item")
@KeySequence("lunaerp_production_yarn_requisition_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRequisitionItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    @TableId
    private Long id;

    /**
     * 所属领纱单ID
     */
    private Long requisitionId;

    /**
     * 纱线材质ID
     */
    private Long yarnTypeId;

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 纱线规格名称快照
     */
    private String yarnSpecName;

    /**
     * 需求量（KG）
     */
    private BigDecimal requiredQty;

    /**
     * 已发量（KG，缺省 0）
     */
    private BigDecimal dispatchedQty;

    /**
     * 单位，固定 KG
     */
    private String unit;

    /**
     * 备注
     */
    private String remark;

}

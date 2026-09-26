package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产管理 - 领纱单主表 DO
 *
 * 表：lunaerp_production_yarn_requisition
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_requisition")
@KeySequence("lunaerp_production_yarn_requisition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRequisitionDO extends TenantBaseDO {

    /**
     * 领纱单ID
     */
    @TableId
    private Long id;

    /**
     * 领纱单号（派生式：YS-{织造单号去符号}-{单内 2 位序号}）
     */
    private String code;

    /**
     * 关联织造单ID
     */
    private Long weavingOrderId;

    /**
     * 领纱单状态
     *
     * DRAFT 草稿 / ACTIVE 待发纱 / PARTIAL 部分发纱 / DISPATCHED 已发纱 / CLOSED 已关闭
     * 字典：lunaerp_yarn_requisition_status
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}

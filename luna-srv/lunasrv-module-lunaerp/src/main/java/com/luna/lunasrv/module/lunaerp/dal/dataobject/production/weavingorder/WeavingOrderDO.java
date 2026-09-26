package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-织造执行-织造单主表 DO
 *
 * 表：lunaerp_production_weaving_order
 *
 * 织造单从自织（productionMode=SELF）胚布单创建，承载车间一次织造执行。
 * 单号派生式：ZZ-{胚布单号去符号}-{该胚布单内2位序号}，租户内全量唯一含软删行（ADR-0023）。
 * 状态：DRAFT 待排产 / ACTIVE 生产中 / COMPLETED 已完工 / CANCELLED 已取消。
 *
 * @author luna
 */
@TableName("lunaerp_production_weaving_order")
@KeySequence("lunaerp_production_weaving_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WeavingOrderDO extends TenantBaseDO {

    /**
     * 织造单ID
     */
    private Long id;

    /**
     * 织造单号（派生式：ZZ-{胚布单号去符号}-{2位序号}，租户内全量唯一含软删行，见 ADR-0023）
     */
    private String code;

    /**
     * 关联胚布单ID（必填，限 productionMode=SELF 且未取消）
     */
    private Long greigeOrderId;

    /**
     * 胚布单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String greigeOrderNo;

    /**
     * 销售合同号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;

    /**
     * 产品档案ID（必填）
     */
    private Long productId;

    /**
     * 产品编号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;

    /**
     * 产品名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productName;

    /**
     * 成分快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String composition;

    /**
     * 克重快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gsm;

    /**
     * 门幅快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String width;

    /**
     * 计划匹数（正整数）
     */
    private Integer plannedQty;

    /**
     * 计划总重量 KG
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal plannedWeight;

    /**
     * 织造单状态（字典 lunaerp_weaving_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED），创建缺省 DRAFT
     */
    private String status;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

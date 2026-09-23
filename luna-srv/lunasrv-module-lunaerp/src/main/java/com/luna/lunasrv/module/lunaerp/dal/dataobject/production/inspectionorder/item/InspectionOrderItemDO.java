package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-检验单录检明细 DO
 *
 * 表：lunaerp_production_inspection_order_item
 *
 * 录检明细作为独立子资源处理（ADR-0014）。
 * 单内 (vatNo, rollNo) 唯一性由 service 层校验，不建数据库唯一约束。
 * 本期不做 defects 疵点字段。
 *
 * @author luna
 */
@TableName("lunaerp_production_inspection_order_item")
@KeySequence("lunaerp_production_inspection_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionOrderItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 检验单ID（lunaerp_production_inspection_order.id）
     */
    private Long inspectionOrderId;

    /**
     * 卷号
     */
    private String rollNo;

    /**
     * 缸号
     */
    private String vatNo;

    /**
     * 实称重量 KG
     */
    private BigDecimal measuredWeight;

    /**
     * 米数
     */
    private BigDecimal lengthM;

    /**
     * 机台号
     */
    private String machineNo;

    /**
     * 等级（字典 lunaerp_inspection_grade：A/B/C/DEFECTIVE）
     */
    private String grade;

    /**
     * 颜色名称
     */
    private String colorName;

    /**
     * 产品色号
     */
    private String colorNo;

    /**
     * 款号
     */
    private String styleNo;

    /**
     * 产品编码快照
     */
    private String productCode;

    /**
     * 产品名称快照
     */
    private String productName;

    /**
     * 匹数（缺省 1）
     */
    private Integer pieceCount;

    /**
     * 备注
     */
    private String remark;

}

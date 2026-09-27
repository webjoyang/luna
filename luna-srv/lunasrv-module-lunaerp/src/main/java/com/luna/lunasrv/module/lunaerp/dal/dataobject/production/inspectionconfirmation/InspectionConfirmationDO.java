package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产管理-质检确认表 DO
 *
 * 表：lunaerp_production_inspection_confirmation
 * 认定链 chain_key 标识同卷多轮认定与撤销改判留痕（ADR-0020）。
 * 所有行一律不可删除，误生成以免扣（WAIVED）结案。
 *
 * @author luna
 */
@TableName("lunaerp_production_inspection_confirmation")
@KeySequence("lunaerp_production_inspection_confirmation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionConfirmationDO extends TenantBaseDO {

    /**
     * 质检确认记录ID
     */
    private Long id;

    /**
     * 认定链标识（首行由服务端预生成，撤销克隆沿用首行值，同链多行同值）
     */
    private String chainKey;

    /**
     * 来源（INSPECTION 检验单引入 / MANUAL 手工登记）
     */
    private String source;

    /**
     * 状态（字典 lunaerp_inspection_confirmation_status：PENDING 待确认 / CONFIRMED 已认定 / REVOKED 已撤销）
     */
    private String status;

    /**
     * 来源检验单ID（lunaerp_production_inspection_order.id，逻辑关联）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionOrderId;

    /**
     * 检验单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionOrderCode;

    /**
     * 来源录检明细ID（lunaerp_production_inspection_order_item.id，INSPECTION 必填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long inspectionOrderItemId;

    /**
     * 卷号快照（MANUAL 亦必填）
     */
    private String rollNo;

    /**
     * 缸号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String vatNo;

    /**
     * 等级快照（复用字典 lunaerp_inspection_grade）
     */
    private String grade;

    /**
     * 机台号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String machineNo;

    /**
     * 产品编码快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;

    /**
     * 产品名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productName;

    /**
     * 颜色名称快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorName;

    /**
     * 产品色号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorNo;

    /**
     * 款号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String styleNo;

    /**
     * 实称重量 KG 快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal measuredWeight;

    /**
     * 米数快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal lengthM;

    /**
     * 责任类型（字典 lunaerp_inspection_responsibility：EQUIPMENT 设备故障（免扣）/ OPERATOR 操作失误（扣款）/ WAIVED 免扣）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String responsibility;

    /**
     * 挡车工用户ID（关联 system_users，OPERATOR 时必填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long workerUserId;

    /**
     * 挡车工姓名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String workerName;

    /**
     * 认定人用户ID（当前登录用户）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long confirmedByUserId;

    /**
     * 认定人姓名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmedByName;

    /**
     * 认定时间（手工登记可补录）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime confirmedAt;

    /**
     * 原因备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String note;

    /**
     * 撤销人用户ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long revokedByUserId;

    /**
     * 撤销人姓名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String revokedByName;

    /**
     * 撤销时间（非空即 REVOKED 冻结行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime revokedAt;

}

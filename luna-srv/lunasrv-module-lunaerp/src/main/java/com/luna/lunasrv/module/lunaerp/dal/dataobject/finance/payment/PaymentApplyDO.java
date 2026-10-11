package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 付款申请单 DO
 *
 * 表：lunaerp_finance_payment_apply
 *
 * 单号 FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行（ADR-0006/0015）；
 * 类型：RECON 对账付款 / PREPAY 预付付款；走 02 号审批引擎。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment_apply")
@KeySequence("lunaerp_finance_payment_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApplyDO extends TenantBaseDO {

    /**
     * 付款申请ID
     */
    private Long id;

    /**
     * 付款申请单号（FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行）
     */
    private String applyNo;

    /**
     * 申请类型（字典 lunaerp_payment_apply_type：RECON 对账付款 / PREPAY 预付付款）
     */
    private String applyType;

    /**
     * 供应商ID（关联 lunaerp_baseinfo_supplier.id）
     */
    private Long supplierId;

    /**
     * 供应商公司名快照
     */
    private String supplierName;

    /**
     * 关联对账单ID（RECON 类型必填，关联 lunaerp_finance_supplier_recon.id）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reconId;

    /**
     * 关联对账单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reconNo;

    /**
     * 本次申请付款金额（元，必须 >0）
     */
    private BigDecimal amount;

    /**
     * 付款用途（PREPAY 类型必填，如纱线采购定金）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String purpose;

    /**
     * 开户行名称快照（展示用，脱敏字段）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankName;

    /**
     * 银行账号快照（展示用，脱敏字段）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankAccount;

    /**
     * 审批状态（02号引擎状态：PENDING待提交/APPROVING审批中/APPROVED已通过/REJECTED已驳回/CANCELLED已撤回）
     */
    private String approvalStatus;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

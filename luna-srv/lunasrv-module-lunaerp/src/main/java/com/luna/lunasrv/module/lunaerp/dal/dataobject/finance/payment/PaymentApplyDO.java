package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务管理 - 付款申请单 DO
 *
 * 表：lunaerp_finance_payment_apply
 *
 * 单号 FK+YYYYMMDD+3 位日流水（ADR-0006 模式），租户内全量唯一含软删行。
 * 类型 RECON 对账付款（必须挂 CONFIRMED 对账单，累计申请额 ≤ 确认金额）/ PREPAY 预付付款（必填用途）。
 * 走 02 号审批引擎链，approval_status 五态；APPROVED 不触发过账（付款登记才扣款记账）。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment_apply")
@KeySequence("lunaerp_finance_payment_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentApplyDO extends TenantBaseDO {

    /**
     * 付款申请单ID
     */
    private Long id;

    /**
     * 申请单号（FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行）
     */
    private String applyNo;

    /**
     * 申请类型（字典 lunaerp_payment_apply_type：RECON 对账付款 / PREPAY 预付付款）
     */
    private String applyType;

    /**
     * 供应商ID（lunaerp_baseinfo_supplier.id）
     */
    private Long supplierId;

    /**
     * 供应商公司名快照
     */
    private String supplierName;

    /**
     * 关联合同对账单ID（RECON 必填，必须为 CONFIRMED 状态）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reconId;

    /**
     * 对账单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reconNo;

    /**
     * 本次申请付款金额（元，必须 >0）
     */
    private BigDecimal amount;

    /**
     * 预付用途备注（PREPAY 必填，如纱线采购定金）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String purpose;

    /**
     * 收款银行名称快照（从供应商档案快照，脱敏位）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankName;

    /**
     * 收款银行账号快照（从供应商档案快照，脱敏位）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankAccount;

    /**
     * 审批状态（PENDING 草稿待提交 / APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回）
     */
    private String approvalStatus;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

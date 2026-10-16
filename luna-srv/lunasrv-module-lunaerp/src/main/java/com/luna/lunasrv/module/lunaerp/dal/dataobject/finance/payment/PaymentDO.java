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
 * 财务管理 - 收付款单主表 DO（收付款共享表）
 *
 * 表：lunaerp_finance_payment
 *
 * 收款（客户）与付款（供应商）共用单据，按 direction=IN/OUT 分型；
 * 本 spec 定义 OUT 侧最小列集：payment_no 全量唯一含软删行；
 * 两步制出纳登记过账：扣资金账户、写付款单与核销明细、写账户流水与分录账；
 * 已登记付款单不可修改删除（凭证），红冲二期。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment")
@KeySequence("lunaerp_finance_payment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentDO extends TenantBaseDO {

    /**
     * 收付款单ID
     */
    private Long id;

    /**
     * 收付款单号（租户内全量唯一含软删行，前缀挂账收口定稿）
     */
    private String paymentNo;

    /**
     * 收付款方向（字典 lunaerp_finance_payment_direction：IN 收款 / OUT 付款）
     */
    private String direction;

    /**
     * 供应商ID（direction=OUT 付款时必填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    /**
     * 客户ID（direction=IN 收款时必填，10 号 IN 侧使用）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customerId;

    /**
     * 资金账户ID（lunaerp_finance_account.id）
     */
    private Long accountId;

    /**
     * 本次资金实付/实收金额（元，扣减资金账户金额）
     */
    private BigDecimal payAmount;

    /**
     * 预付/预收抵扣金额（元，预付款抵扣额）
     */
    private BigDecimal prepayUsedAmount;

    /**
     * 状态（COMPLETED 登记完成 / REVERSED 红冲取消预留）
     */
    private String status;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /**
     * 收款单类型（字典 lunaerp_finance_receipt_type：RECEIPT 货款 / ADVANCE 预收；OUT 侧恒空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String receiptType;

    /**
     * 收款币种（复用销售合同币种/外币，OUT 一期为 RMB）
     */
    private String currency;

    /**
     * 过账日汇率（1外币 = 汇率本位币，RMB 为 1.00000000）
     */
    private BigDecimal exchangeRate;

    /**
     * 收款日期（进分录账 biz_date）
     */
    private java.time.LocalDate receiptDate;

    /**
     * 银行回单号/交易流水号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankRefNo;

    /**
     * 经手人姓名
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String handlerName;

    /**
     * 审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED；OUT 侧不走审批）
     */
    private String approvalStatus;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 工资发放批次表 DO
 *
 * 表：lunaerp_finance_payroll_payment
 *
 * @author luna
 */
@TableName("lunaerp_finance_payroll_payment")
@KeySequence("lunaerp_finance_payroll_payment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollPaymentDO extends TenantBaseDO {

    /**
     * 发放批次ID
     */
    private Long id;

    /**
     * 发放流水号（GF+YYYYMMDD+3位流水，全量唯一含软删行）
     */
    private String paymentNo;

    /**
     * 工资单主表ID（lunaerp_finance_payroll.id）
     */
    private Long payrollId;

    /**
     * 工资单号快照
     */
    private String payrollNo;

    /**
     * 工资期间快照（YYYY-MM）
     */
    private String period;

    /**
     * 付款资金账户ID（lunaerp_finance_account.id）
     */
    private Long accountId;

    /**
     * 付款资金账户名称快照
     */
    private String accountName;

    /**
     * 币种（RMB）
     */
    private String currency;

    /**
     * 原币金额（元，发放金额必须 > 0）
     */
    private BigDecimal originalAmount;

    /**
     * 汇率快照（RMB恒为1.00000000）
     */
    private BigDecimal exchangeRate;

    /**
     * 本位币金额（元）
     */
    private BigDecimal baseAmount;

    /**
     * 发放支付日期
     */
    private LocalDate payDate;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务管理 - 工资单员工汇总行表 DO
 *
 * 表：lunaerp_finance_payroll_employee
 *
 * @author luna
 */
@TableName("lunaerp_finance_payroll_employee")
@KeySequence("lunaerp_finance_payroll_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollEmployeeDO extends TenantBaseDO {

    /**
     * 员工汇总行ID
     */
    private Long id;

    /**
     * 工资单主表ID（lunaerp_finance_payroll.id）
     */
    private Long payrollId;

    /**
     * 员工用户ID（system_users.id）
     */
    private Long userId;

    /**
     * 员工昵称快照
     */
    private String nickname;

    /**
     * 员工应发合计（元，服务端权威重算 = Σ款项行应发）
     */
    private BigDecimal earningTotal;

    /**
     * 员工扣款合计（元，服务端权威重算 = Σ款项行扣款）
     */
    private BigDecimal deductionTotal;

    /**
     * 员工实发合计（元，服务端权威重算 = 应发合计 - 扣款合计，不可为负）
     */
    private BigDecimal netAmount;

    /**
     * 员工备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

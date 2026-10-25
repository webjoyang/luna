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
 * 财务管理 - 工资单款项明细行表 DO
 *
 * 表：lunaerp_finance_payroll_line_item
 *
 * @author luna
 */
@TableName("lunaerp_finance_payroll_line_item")
@KeySequence("lunaerp_finance_payroll_line_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollLineItemDO extends TenantBaseDO {

    /**
     * 款项明细行ID
     */
    private Long id;

    /**
     * 工资单主表ID（lunaerp_finance_payroll.id）
     */
    private Long payrollId;

    /**
     * 员工汇总行ID（lunaerp_finance_payroll_employee.id）
     */
    private Long employeeRowId;

    /**
     * 款项大类（EARNING 收入 / DEDUCTION 扣款）
     */
    private String itemKind;

    /**
     * 款项类型（EARNING 字典 2285；DEDUCTION 字典 2286）
     */
    private String itemType;

    /**
     * 计件数量（可空，计件项适用）
     */
    private BigDecimal qty;

    /**
     * 计件单价（元，可空，计件项适用）
     */
    private BigDecimal unitPrice;

    /**
     * 款项金额（元，必须 > 0，有数量单价时服务端自动计算）
     */
    private BigDecimal amount;

    /**
     * 款项说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense;

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
 * 财务管理 - 员工费用报销明细行 DO
 *
 * 表：lunaerp_finance_expense_item
 *
 * @author luna
 */
@TableName("lunaerp_finance_expense_item")
@KeySequence("lunaerp_finance_expense_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseItemDO extends TenantBaseDO {

    /**
     * 明细行ID
     */
    private Long id;

    /**
     * 报销单ID
     */
    private Long expenseId;

    /**
     * 费用类别（字典 2284 lunaerp_expense_category：TRAVEL/OFFICE/TRANSPORT/ENTERTAIN/RENT/UTILITY/COMM/OTHER）
     */
    private String category;

    /**
     * 原币金额（元，必须 > 0）
     */
    private BigDecimal originalAmount;

    /**
     * 费用发生日期（不晚于提交日）
     */
    private LocalDate occurDate;

    /**
     * 费用明细备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

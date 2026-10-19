package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务管理 - 员工费用报销主表 DO
 *
 * 表：lunaerp_finance_expense
 *
 * @author luna
 */
@TableName(value = "lunaerp_finance_expense", autoResultMap = true)
@KeySequence("lunaerp_finance_expense_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseDO extends TenantBaseDO {

    /**
     * 报销单ID
     */
    private Long id;

    /**
     * 报销单号（BX+YYYYMMDD+3位流水，租户内全量唯一含软删行）
     */
    private String expenseNo;

    /**
     * 报销人用户ID（system_users.id）
     */
    private Long applicantUserId;

    /**
     * 报销人姓名快照
     */
    private String applicantName;

    /**
     * 付款资金账户ID（lunaerp_finance_account.id）
     */
    private Long accountId;

    /**
     * 付款资金账户名称快照
     */
    private String accountName;

    /**
     * 币种（与账户币种联动一致）
     */
    private String currency;

    /**
     * 原币总金额（元，服务端重算 = Σ行金额）
     */
    private BigDecimal originalTotal;

    /**
     * 审批通过日汇率快照（1外币 = 汇率本位币，RMB 为 1.00000000）
     */
    private BigDecimal exchangeRate;

    /**
     * 本位币总金额（元，折算后金额）
     */
    private BigDecimal baseTotal;

    /**
     * 报销总金额（元，账户为RMB时与original_total相同）
     */
    private BigDecimal totalAmount;

    /**
     * 报销业务日期（取明细行内最大发生日期）
     */
    private LocalDate expenseDate;

    /**
     * 审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）
     */
    private String approvalStatus;

    /**
     * 提交审批时间
     */
    private LocalDateTime submitTime;

    /**
     * 审批通过时间
     */
    private LocalDateTime approveTime;

    /**
     * 附件列表JSONB（有序文件URL数组，最多10个）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

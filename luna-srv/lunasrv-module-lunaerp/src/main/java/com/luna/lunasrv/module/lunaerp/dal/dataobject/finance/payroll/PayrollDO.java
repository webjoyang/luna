package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll;

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
 * 财务管理 - 员工工资单主表 DO
 *
 * 表：lunaerp_finance_payroll
 *
 * @author luna
 */
@TableName(value = "lunaerp_finance_payroll", autoResultMap = true)
@KeySequence("lunaerp_finance_payroll_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollDO extends TenantBaseDO {

    /**
     * 工资单ID
     */
    private Long id;

    /**
     * 工资单号（GZ+YYYYMM+3位流水，租户内全量唯一含软删行）
     */
    private String payrollNo;

    /**
     * 工资期间（YYYY-MM格式年月）
     */
    private String period;

    /**
     * 工资单标题（缺省"{period} 工资表"）
     */
    private String title;

    /**
     * 审批状态（02号引擎五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）
     */
    private String approvalStatus;

    /**
     * 应发总计（元，服务端权威重算 = Σ员工应发）
     */
    private BigDecimal earningTotal;

    /**
     * 扣款总计（元，服务端权威重算 = Σ员工扣款）
     */
    private BigDecimal deductionTotal;

    /**
     * 实发总计（元，服务端权威重算 = 应发总计 - 扣款总计）
     */
    private BigDecimal netAmount;

    /**
     * 提交审批时间
     */
    private LocalDateTime submitTime;

    /**
     * 审批通过时间
     */
    private LocalDateTime approveTime;

    /**
     * 作废操作人
     */
    private String cancelledBy;

    /**
     * 作废时间
     */
    private LocalDateTime cancelledAt;

    /**
     * 作废原因（作废时必填）
     */
    private String cancelReason;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户对账单主表 DO
 *
 * 表：lunaerp_finance_statement
 *
 * @author luna
 */
@TableName("lunaerp_finance_statement")
@KeySequence("lunaerp_finance_statement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerStatementDO extends TenantBaseDO {

    /**
     * 对账单ID
     */
    private Long id;

    /**
     * 对账单号（CS+YYYY+4位流水，租户内全量唯一含软删行）
     */
    private String statementNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户公司名快照
     */
    private String customerName;

    /**
     * 对账币种（多币种分单生成）
     */
    private String currency;

    /**
     * 对账区间起始日期
     */
    private LocalDate periodStart;

    /**
     * 对账区间截止日期
     */
    private LocalDate periodEnd;

    /**
     * 期初欠款余额（元，服务端重算）
     */
    private BigDecimal openingBalance;

    /**
     * 本期应收发生额（借方合计）
     */
    private BigDecimal periodDebit;

    /**
     * 本期收款核销额（贷方合计）
     */
    private BigDecimal periodCredit;

    /**
     * 期末应收余额（元，期初+借-贷）
     */
    private BigDecimal closingBalance;

    /**
     * 状态（DRAFT/SENT/CONFIRMED/REJECTED/VOID）
     */
    private String status;

    /**
     * 确认方式（ONLINE_PORTAL/ON_BEHALF）
     */
    private String confirmType;

    /**
     * 确认人姓名快照
     */
    private String confirmByName;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 确认客户端IP
     */
    private String confirmIp;

    /**
     * 线下代客确认方式（微信/电话/邮件/纸质签字等）
     */
    private String confirmMethod;

    /**
     * 异议/驳回原因
     */
    private String rejectReason;

    /**
     * 财务回复异议备注
     */
    private String replyRemark;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

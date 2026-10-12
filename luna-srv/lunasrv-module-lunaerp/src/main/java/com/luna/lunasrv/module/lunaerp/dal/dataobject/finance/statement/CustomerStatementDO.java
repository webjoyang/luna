package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务管理 - 客户对账单主表 DO
 *
 * 表：lunaerp_finance_statement
 *
 * @author luna
 */
@TableName("lunaerp_finance_statement")
@KeySequence("lunaerp_finance_statement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementDO extends TenantBaseDO {

    /**
     * 对账单ID
     */
    private Long id;

    /**
     * 对账单号（CS+YYYYMMDD+4位流水等，租户内全量唯一含软删行）
     */
    private String statementNo;

    /**
     * 客户ID（关联 lunaerp_baseinfo_customer.id）
     */
    private Long customerId;

    /**
     * 客户名称快照
     */
    private String customerName;

    /**
     * 币种（CNY / USD 等）
     */
    private String currency;

    /**
     * 对账期间开始日期
     */
    private LocalDate periodStart;

    /**
     * 对账期间结束日期
     */
    private LocalDate periodEnd;

    /**
     * 期初余额
     */
    private BigDecimal openingBalance;

    /**
     * 本期借方发生额（本期发货/应收增加）
     */
    private BigDecimal periodDebit;

    /**
     * 本期贷方发生额（本期回款/冲减）
     */
    private BigDecimal periodCredit;

    /**
     * 期末余额
     */
    private BigDecimal closingBalance;

    /**
     * 状态（DRAFT 草稿 / SENT 已发送 / CONFIRMED 已确认 / REJECTED 已驳回 / VOID 已作废）
     */
    private String status;

    /**
     * 确认方式类型（ONLINE_PORTAL 线上门户 / ON_BEHALF 内部代客）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmType;

    /**
     * 确认人姓名（门户客户手填或内部代客经手人）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmByName;

    /**
     * 确认时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime confirmTime;

    /**
     * 确认IP（门户确认时记录客户端IP）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmIp;

    /**
     * 确认方式/凭证备注（内部代客时填写）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmMethod;

    /**
     * 驳回原因（门户客户手填驳回原因）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectReason;

    /**
     * 驳回复核备注（财务处理时填写）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String replyRemark;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

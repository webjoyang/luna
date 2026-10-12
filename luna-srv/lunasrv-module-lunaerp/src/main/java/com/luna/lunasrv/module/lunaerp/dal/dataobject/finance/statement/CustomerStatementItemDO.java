package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 客户对账单明细行表 DO
 *
 * 表：lunaerp_finance_statement_item
 *
 * @author luna
 */
@TableName("lunaerp_finance_statement_item")
@KeySequence("lunaerp_finance_statement_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementItemDO extends TenantBaseDO {

    /**
     * 对账单明细ID
     */
    private Long id;

    /**
     * 关联对账单主表ID（lunaerp_finance_statement.id）
     */
    private Long statementId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 关联分录ID（引用财务分录账，可选）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ledgerEntryId;

    /**
     * 借贷方向（DEBIT 借方 / CREDIT 贷方）
     */
    private String direction;

    /**
     * 业务日期
     */
    private LocalDate bizDate;

    /**
     * 源单类型（SHIPMENT / RECEIPT 等）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String refType;

    /**
     * 源单单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String refNo;

    /**
     * 摘要 / 品名或事由
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    /**
     * 借方发生额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方发生额
     */
    private BigDecimal creditAmount;

    /**
     * 签收状态快照标记
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String signStatus;

    /**
     * 行备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

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

/**
 * 客户对账单明细行快照 DO
 *
 * 表：lunaerp_finance_statement_item
 *
 * @author luna
 */
@TableName("lunaerp_finance_statement_item")
@KeySequence("lunaerp_finance_statement_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerStatementItemDO extends TenantBaseDO {

    /**
     * 明细行ID
     */
    private Long id;

    /**
     * 所属对账单ID（lunaerp_finance_statement.id）
     */
    private Long statementId;

    /**
     * 行号（从1递增）
     */
    private Integer lineNo;

    /**
     * 被引用的经营分录账ID（lunaerp_finance_ledger_entry.id）
     */
    private Long ledgerEntryId;

    /**
     * 往来方向（DEBIT 借 / CREDIT 贷）
     */
    private String direction;

    /**
     * 业务日期快照
     */
    private LocalDate bizDate;

    /**
     * 来源单据类型快照（SHIPMENT / RECEIPT 等）
     */
    private String refType;

    /**
     * 来源业务单号快照
     */
    private String refNo;

    /**
     * 摘要（品名或事由）
     */
    private String summary;

    /**
     * 借方金额（元，应收增加）
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额（元，应收减少）
     */
    private BigDecimal creditAmount;

    /**
     * 签收状态快照标记
     */
    private String signStatus;

    /**
     * 行备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 经营分录账 DO（域公共，append-only 唯一账本，ADR-0043）
 *
 * 表：lunaerp_finance_ledger_entry
 *
 * @author luna
 */
@TableName("lunaerp_finance_ledger_entry")
@KeySequence("lunaerp_finance_ledger_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class LedgerEntryDO extends TenantBaseDO {

    /**
     * 分录ID
     */
    private Long id;

    /**
     * 业务日期（发货日/收款日/操作日）
     */
    private LocalDate bizDate;

    /**
     * 往来方类型（CUSTOMER 客户 / SUPPLIER 供应商）
     */
    private String partyType;

    /**
     * 往来方ID（客户ID或供应商ID）
     */
    private Long partyId;

    /**
     * 资金账户ID（非空表示资金流水视图行，空表示往来分录行）
     */
    private Long accountId;

    /**
     * 分录类别（字典 lunaerp_finance_ledger_category，如 AR_INC, AR_INC_REVERSAL 等）
     */
    private String category;

    /**
     * 往来方向（DEBIT 借方往来增加 / CREDIT 贷方往来减少）
     */
    private String direction;

    /**
     * 币种（RMB/USD/EUR/HKD 等）
     */
    private String currency;

    /**
     * 入账汇率（1 外币 = 汇率本位币，RMB 恒为 1.00000000）
     */
    private BigDecimal exchangeRate;

    /**
     * 原币金额（元）
     */
    private BigDecimal originalAmount;

    /**
     * 本位币金额（元）
     */
    private BigDecimal baseAmount;

    /**
     * 业务回指类型（SHIPMENT_ITEM / SHIPMENT_FREIGHT / RECEIPT 等）
     */
    private String refType;

    /**
     * 业务回指主单ID
     */
    private Long refId;

    /**
     * 业务回指明细ID
     */
    private Long refItemId;

    /**
     * 红冲回指原分录ID
     */
    private Long reversalOfId;

    /**
     * 备注说明
     */
    private String remark;

}

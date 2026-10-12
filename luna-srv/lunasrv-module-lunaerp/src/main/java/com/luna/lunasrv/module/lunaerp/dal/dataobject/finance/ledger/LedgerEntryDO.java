package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 经营分录账 DO（域公共 append-only 唯一流水账）
 *
 * 表：lunaerp_finance_ledger_entry
 * 序列：lunaerp_finance_ledger_entry_seq
 *
 * @author luna
 */
@TableName("lunaerp_finance_ledger_entry")
@KeySequence("lunaerp_finance_ledger_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 往来主体类型（CUSTOMER/SUPPLIER）
     */
    private String partyType;

    /**
     * 往来主体ID（客户ID/供应商ID）
     */
    private Long partyId;

    /**
     * 资金账户ID（非空即资金流水视图行）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long accountId;

    /**
     * 分录类别（字典 lunaerp_finance_ledger_category 2274）
     */
    private String category;

    /**
     * 借贷方向（DEBIT 借方往来增加 / CREDIT 贷方往来减少）
     */
    private String direction;

    /**
     * 币种
     */
    private String currency;

    /**
     * 入账汇率（1 外币兑本位币，默认 1）
     */
    private BigDecimal exchangeRate;

    /**
     * 原币金额
     */
    private BigDecimal originalAmount;

    /**
     * 本位币金额（人民币）
     */
    private BigDecimal baseAmount;

    /**
     * 业务源单类型（SHIPMENT/SHIPMENT_ITEM/SHIPMENT_FREIGHT/PAYMENT/RETURN_ORDER等）
     */
    private String refType;

    /**
     * 业务源单ID
     */
    private Long refId;

    /**
     * 业务源单明细行ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long refItemId;

    /**
     * 红冲回指原分录ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reversalOfId;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务管理 - 收付款核销明细表 DO（共享表）
 *
 * 表：lunaerp_finance_payment_ref
 *
 * 记录收付款单对上游申请或应收/应付分录的分配核销明细；
 * OUT 侧 ref_type='PAY_APPLY'，ref_id=付款申请ID，allocate_amount=分配核销额。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment_ref")
@KeySequence("lunaerp_finance_payment_ref_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRefDO extends TenantBaseDO {

    /**
     * 核销明细ID
     */
    private Long id;

    /**
     * 所属收付款单ID（lunaerp_finance_payment.id）
     */
    private Long paymentId;

    /**
     * 核销业务类型（PAY_APPLY 付款申请；10 号扩展 AR_ENTRY 应收分录）
     */
    private String refType;

    /**
     * 关联业务单据ID（付款申请ID / 业务分录ID）
     */
    private Long refId;

    /**
     * 关联付款申请单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String applyNo;

    /**
     * 本次核销分配金额（元）
     */
    private BigDecimal allocateAmount;

    /**
     * 分录编号/业务单号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String entryNo;

    /**
     * 业务日期快照
     */
    private java.time.LocalDate bizDate;

    /**
     * 本位币核销金额快照（元）
     */
    private BigDecimal baseAmount;

    /**
     * 核销分录回指ID（lunaerp_finance_ledger_entry.id，OUT 侧恒空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ledgerEntryId;

}

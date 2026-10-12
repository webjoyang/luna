package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 收付款核销子表 DO（应收IN/应付OUT共享表）
 *
 * 表：lunaerp_finance_payment_ref
 *
 * 记录收付款单与上游申请/发货明细之间的核销分配关系；
 * OUT 侧 ref_type=PAY_APPLY 核销付款申请；IN 侧 ref_type=AR_ENTRY 核销应收分录。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment_ref")
@KeySequence("lunaerp_finance_payment_ref_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefDO extends TenantBaseDO {

    /**
     * 核销明细ID
     */
    private Long id;

    /**
     * 所属收付款单ID（关联 lunaerp_finance_payment.id）
     */
    private Long paymentId;

    /**
     * 核销目标类型（PAY_APPLY 付款申请 / AR_ENTRY 应收分录）
     */
    private String refType;

    /**
     * 核销目标记录ID（付款申请ID / 经营分录ID）
     */
    private Long refId;

    /**
     * 付款申请单号快照（PAY_APPLY 时记录）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String applyNo;

    /**
     * 本次核销分配金额（元）
     */
    private BigDecimal allocateAmount;

    /**
     * 应收分录单号快照（IN 侧 ref_type=AR_ENTRY 时记录）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String entryNo;

    /**
     * 业务日期快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDate bizDate;

    /**
     * 折算本位币金额（人民币）
     */
    private BigDecimal baseAmount;

    /**
     * 关联核销分录ID（引用 lunaerp_finance_ledger_entry.id）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ledgerEntryId;

}

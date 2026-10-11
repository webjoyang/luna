package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 收付款单主表 DO（应收IN/应付OUT共享表）
 *
 * 表：lunaerp_finance_payment
 *
 * 单号 PAY+YYYYMMDD+3位日流水，租户内全量唯一含软删行；
 * 本表为应收（IN 客户收款）与应付（OUT 供应商付款）共享表；
 * direction 区分：IN 收款 / OUT 付款。
 *
 * @author luna
 */
@TableName("lunaerp_finance_payment")
@KeySequence("lunaerp_finance_payment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDO extends TenantBaseDO {

    /**
     * 收付款单ID
     */
    private Long id;

    /**
     * 收付款单号（PAY+YYYYMMDD+3位日流水，租户内全量唯一含软删行）
     */
    private String paymentNo;

    /**
     * 收付款方向（字典 lunaerp_finance_payment_direction：IN 收款 / OUT 付款）
     */
    private String direction;

    /**
     * 供应商ID（OUT 付款时使用，关联 lunaerp_baseinfo_supplier.id）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    /**
     * 客户ID（IN 收款时使用，关联 lunaerp_baseinfo_customer.id）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customerId;

    /**
     * 资金账户ID（关联 lunaerp_finance_account.id）
     */
    private Long accountId;

    /**
     * 本次资金实收/实付金额（元）
     */
    private BigDecimal payAmount;

    /**
     * 预收/预付款抵扣金额（元，缺省0）
     */
    private BigDecimal prepayUsedAmount;

    /**
     * 状态（COMPLETED 已登记过账 / REVERSED 已红冲预留）
     */
    private String status;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

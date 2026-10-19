package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account;

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
 * 财务管理 - 其他收支单 DO（无单据收支凭证）
 *
 * 表：lunaerp_finance_other_txn
 *
 * @author luna
 */
@TableName("lunaerp_finance_other_txn")
@KeySequence("lunaerp_finance_other_txn_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class OtherTxnDO extends TenantBaseDO {

    /**
     * 其他收支单ID
     */
    private Long id;

    /**
     * 其他收支单号（QT+YYYYMMDD+3位流水，租户内全量唯一含软删行）
     */
    private String txnNo;

    /**
     * 收支资金账户ID（lunaerp_finance_account.id）
     */
    private Long accountId;

    /**
     * 资金账户名称快照
     */
    private String accountName;

    /**
     * 收支方向（DEBIT 流入 / CREDIT 流出）
     */
    private String direction;

    /**
     * 收支类别（字典 lunaerp_other_txn_category：INTEREST_IN/WASTE_IN/OTHER_IN/FEE_OUT/COST_OUT/OTHER_OUT）
     */
    private String category;

    /**
     * 币种（外币账户按登记日 SPOT 取价，RMB 为 1.00000000）
     */
    private String currency;

    /**
     * 原币金额（元，必须 > 0）
     */
    private BigDecimal originalAmount;

    /**
     * 汇率（1外币 = 汇率本位币）
     */
    private BigDecimal exchangeRate;

    /**
     * 本位币金额（元）
     */
    private BigDecimal baseAmount;

    /**
     * 收支业务日期
     */
    private LocalDate txnDate;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

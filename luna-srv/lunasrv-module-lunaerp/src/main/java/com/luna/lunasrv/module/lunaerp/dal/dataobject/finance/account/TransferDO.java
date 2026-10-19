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
 * 财务管理 - 资金账户间转账单 DO
 *
 * 表：lunaerp_finance_transfer
 *
 * @author luna
 */
@TableName("lunaerp_finance_transfer")
@KeySequence("lunaerp_finance_transfer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferDO extends TenantBaseDO {

    /**
     * 转账单ID
     */
    private Long id;

    /**
     * 转账单号（ZZ+YYYYMMDD+3位流水，租户内全量唯一含软删行）
     */
    private String transferNo;

    /**
     * 转出资金账户ID（lunaerp_finance_account.id）
     */
    private Long fromAccountId;

    /**
     * 转出账户名称快照
     */
    private String fromAccountName;

    /**
     * 转入资金账户ID（lunaerp_finance_account.id）
     */
    private Long toAccountId;

    /**
     * 转入账户名称快照
     */
    private String toAccountName;

    /**
     * 转账币种（必须同币种转账）
     */
    private String currency;

    /**
     * 转账金额（元，必须 > 0）
     */
    private BigDecimal amount;

    /**
     * 转账业务日期
     */
    private LocalDate transferDate;

    /**
     * 状态（COMPLETED 固定登记即完成，错账红冲二期）
     */
    private String status;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

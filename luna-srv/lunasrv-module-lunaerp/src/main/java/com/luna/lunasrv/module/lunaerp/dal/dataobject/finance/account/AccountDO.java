package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户表 DO（域公共最小集）
 *
 * 表：lunaerp_finance_account
 * 序列：lunaerp_finance_account_seq
 *
 * @author luna
 */
@TableName("lunaerp_finance_account")
@KeySequence("lunaerp_finance_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDO extends TenantBaseDO {

    /**
     * 资金账户ID
     */
    private Long id;

    /**
     * 资金账户名称（租户内唯一）
     */
    private String accountName;

    /**
     * 账户类型（字典 lunaerp_finance_account_type 2277：CASH/BANK/WECHAT/ALIPAY）
     */
    private String accountType;

    /**
     * 账户币种（缺省 CNY）
     */
    private String currency;

    /**
     * 期初余额（仅创建时可录入，后续只读）
     */
    private BigDecimal openingBalance;

    /**
     * 当前余额（仅通过单据过账条件更新变更）
     */
    private BigDecimal currentBalance;

    /**
     * 状态（ENABLE 启用 / DISABLE 停用）
     */
    private String status;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}

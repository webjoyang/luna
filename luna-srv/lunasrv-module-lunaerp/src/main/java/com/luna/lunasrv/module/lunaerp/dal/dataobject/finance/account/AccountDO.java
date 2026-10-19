package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户 DO（域公共最小集，ADR-0043）
 *
 * 表：lunaerp_finance_account
 *
 * @author luna
 */
@TableName("lunaerp_finance_account")
@KeySequence("lunaerp_finance_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountDO extends TenantBaseDO {

    /**
     * 资金账户ID
     */
    private Long id;

    /**
     * 账户名称（租户内全量唯一）
     */
    private String accountName;

    /**
     * 账户类型（字典 lunaerp_finance_account_type：CASH/BANK/WECHAT/ALIPAY）
     */
    private String accountType;

    /**
     * 账号/微信标识/支付宝标识（BANK/WECHAT/ALIPAY 必填，CASH 可空，@Sensitive(CONTACT)）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String accountNo;

    /**
     * 账户性质（字典 lunaerp_finance_account_nature：PUBLIC 公户 / PRIVATE 私户，缺省 PUBLIC）
     */
    private String accountNature;

    /**
     * 账户币种（默认 RMB）
     */
    private String currency;

    /**
     * 期初余额（元，仅创建时录入一次，此后只读）
     */
    private BigDecimal openingBalance;

    /**
     * 当前余额（元，冗余物化字段，仅经单据过账条件更新改变）
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

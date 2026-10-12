package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 资金账户类型枚举
 * 对应字典：lunaerp_finance_account_type（2277，域公共）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinanceAccountTypeEnum {

    /**
     * 现金
     */
    CASH("CASH", "现金"),

    /**
     * 银行存款
     */
    BANK("BANK", "银行存款"),

    /**
     * 微信支付
     */
    WECHAT("WECHAT", "微信支付"),

    /**
     * 支付宝
     */
    ALIPAY("ALIPAY", "支付宝");

    private final String value;
    private final String label;

    public static boolean isValid(String accountType) {
        if (accountType == null) {
            return false;
        }
        for (FinanceAccountTypeEnum e : values()) {
            if (e.getValue().equals(accountType)) {
                return true;
            }
        }
        return false;
    }

}

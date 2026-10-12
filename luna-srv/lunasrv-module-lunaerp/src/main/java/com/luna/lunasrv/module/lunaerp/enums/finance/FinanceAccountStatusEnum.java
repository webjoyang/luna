package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 资金账户状态枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinanceAccountStatusEnum {

    /**
     * 启用
     */
    ENABLE("ENABLE", "启用"),

    /**
     * 停用
     */
    DISABLE("DISABLE", "停用");

    private final String value;
    private final String label;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (FinanceAccountStatusEnum e : values()) {
            if (e.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

}

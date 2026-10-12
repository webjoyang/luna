package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 往来主体类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinancePartyTypeEnum {

    /**
     * 客户
     */
    CUSTOMER("CUSTOMER", "客户"),

    /**
     * 供应商
     */
    SUPPLIER("SUPPLIER", "供应商");

    private final String value;
    private final String label;

    public static boolean isValid(String partyType) {
        if (partyType == null) {
            return false;
        }
        for (FinancePartyTypeEnum e : values()) {
            if (e.getValue().equals(partyType)) {
                return true;
            }
        }
        return false;
    }

}

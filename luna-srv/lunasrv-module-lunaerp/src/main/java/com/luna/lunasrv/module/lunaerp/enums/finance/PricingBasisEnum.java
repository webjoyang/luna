package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 计价基准枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PricingBasisEnum {

    /**
     * 公斤
     */
    KG("KG", "公斤"),

    /**
     * 米
     */
    METER("METER", "米");

    private final String value;
    private final String label;

    public static boolean isValid(String basis) {
        if (basis == null) {
            return false;
        }
        for (PricingBasisEnum e : values()) {
            if (e.getValue().equalsIgnoreCase(basis)) {
                return true;
            }
        }
        return false;
    }

}

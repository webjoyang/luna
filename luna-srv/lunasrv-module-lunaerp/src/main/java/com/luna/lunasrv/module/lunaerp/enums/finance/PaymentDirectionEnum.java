package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 收付款方向枚举
 * 对应字典：lunaerp_finance_payment_direction（2271，共享字典）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentDirectionEnum {

    /**
     * 收款
     */
    IN("IN", "收款"),

    /**
     * 付款
     */
    OUT("OUT", "付款");

    private final String value;
    private final String label;

    public static boolean isValid(String direction) {
        if (direction == null) {
            return false;
        }
        for (PaymentDirectionEnum e : values()) {
            if (e.getValue().equals(direction)) {
                return true;
            }
        }
        return false;
    }

}

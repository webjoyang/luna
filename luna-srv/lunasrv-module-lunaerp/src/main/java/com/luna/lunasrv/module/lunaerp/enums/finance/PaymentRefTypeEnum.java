package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 收付款核销目标类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentRefTypeEnum {

    /**
     * 付款申请（OUT 侧）
     */
    PAY_APPLY("PAY_APPLY", "付款申请"),

    /**
     * 应收分录（IN 侧预留）
     */
    AR_ENTRY("AR_ENTRY", "应收分录");

    private final String value;
    private final String label;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (PaymentRefTypeEnum e : values()) {
            if (e.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

}

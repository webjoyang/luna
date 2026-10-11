package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 收付款单状态枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {

    /**
     * 已登记
     */
    COMPLETED("COMPLETED", "已登记"),

    /**
     * 已红冲
     */
    REVERSED("REVERSED", "已红冲");

    private final String value;
    private final String label;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (PaymentStatusEnum e : values()) {
            if (e.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

}

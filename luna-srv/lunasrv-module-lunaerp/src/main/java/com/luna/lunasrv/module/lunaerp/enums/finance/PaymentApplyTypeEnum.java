package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 付款申请类型枚举
 * 对应字典：lunaerp_payment_apply_type（2270）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentApplyTypeEnum {

    /**
     * 对账付款
     */
    RECON("RECON", "对账付款"),

    /**
     * 预付付款
     */
    PREPAY("PREPAY", "预付付款");

    private final String value;
    private final String label;

    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (PaymentApplyTypeEnum e : values()) {
            if (e.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

}

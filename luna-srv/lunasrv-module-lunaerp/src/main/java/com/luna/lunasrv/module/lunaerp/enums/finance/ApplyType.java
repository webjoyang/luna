package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款申请类型枚举
 * 对应字典：lunaerp_payment_apply_type
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ApplyType {

    /**
     * 对账付款（挂 CONFIRMED 供应商对账单）
     */
    RECON("RECON", "对账付款"),

    /**
     * 预付付款（不挂对账单，必填用途备注）
     */
    PREPAY("PREPAY", "预付付款");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

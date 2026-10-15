package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收付款方向枚举
 * 对应字典：lunaerp_finance_payment_direction（共享字典，10 号收款侧复用）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum Direction {

    /**
     * 收款（客户侧，10 号使用）
     */
    IN("IN", "收款"),

    /**
     * 付款（供应商侧，11 号使用）
     */
    OUT("OUT", "付款");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户对账单确认方式枚举
 * 对应字典：lunaerp_finance_statement_confirm_type (2278)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum CustomerStatementConfirmType {

    /**
     * 门户在线确认
     */
    ONLINE_PORTAL("ONLINE_PORTAL", "在线确认"),

    /**
     * 代客确认
     */
    ON_BEHALF("ON_BEHALF", "代客确认");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 客户对账单确认方式枚举
 * 字典：lunaerp_finance_statement_confirm_type (2278)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum CustomerStatementConfirmTypeEnum {

    /**
     * 线上门户自主确认
     */
    ONLINE_PORTAL("ONLINE_PORTAL", "线上门户"),

    /**
     * 内部代客确认
     */
    ON_BEHALF("ON_BEHALF", "内部代客");

    private final String value;
    private final String label;

}

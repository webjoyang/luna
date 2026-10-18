package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户对账单状态枚举
 * 对应字典：lunaerp_finance_statement_status (2272)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum CustomerStatementStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已发出
     */
    SENT("SENT", "已发出"),

    /**
     * 已确认
     */
    CONFIRMED("CONFIRMED", "已确认"),

    /**
     * 已驳回
     */
    REJECTED("REJECTED", "已驳回"),

    /**
     * 已作废
     */
    VOID("VOID", "已作废");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 客户对账单状态枚举
 * 字典：lunaerp_finance_statement_status (2272)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum CustomerStatementStatusEnum {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已发送
     */
    SENT("SENT", "已发送"),

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

}

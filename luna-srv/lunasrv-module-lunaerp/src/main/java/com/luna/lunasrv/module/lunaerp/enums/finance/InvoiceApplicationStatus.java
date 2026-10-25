package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发票申请状态枚举
 * 对应表字段：lunaerp_finance_invoice_application.status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InvoiceApplicationStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已提交
     */
    SUBMITTED("SUBMITTED", "已提交"),

    /**
     * 已开票
     */
    ISSUED("ISSUED", "已开票"),

    /**
     * 已驳回
     */
    REJECTED("REJECTED", "已驳回");

    private final String value;
    private final String label;
}

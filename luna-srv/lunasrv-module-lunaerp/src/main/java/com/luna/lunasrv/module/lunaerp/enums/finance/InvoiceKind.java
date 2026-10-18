package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发票种类枚举
 * 对应字典：lunaerp_finance_invoice_kind (2275)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InvoiceKind {

    /**
     * 数电专票
     */
    DIGITAL_SPECIAL("DIGITAL_SPECIAL", "数电专票"),

    /**
     * 数电普票
     */
    DIGITAL_NORMAL("DIGITAL_NORMAL", "数电普票"),

    /**
     * 增值税专用发票
     */
    VAT_SPECIAL("VAT_SPECIAL", "增值税专用发票"),

    /**
     * 增值税普通发票
     */
    VAT_NORMAL("VAT_NORMAL", "增值税普通发票");

    private final String value;
    private final String label;
}

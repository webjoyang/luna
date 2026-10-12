package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 发票种类枚举（字典 2275: lunaerp_finance_invoice_kind）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InvoiceKindEnum {

    /**
     * 数电专票
     */
    ELEC_SPECIAL("ELEC_SPECIAL", "数电专票"),

    /**
     * 数电普票
     */
    ELEC_NORMAL("ELEC_NORMAL", "数电普票"),

    /**
     * 增值税专票
     */
    VAT_SPECIAL("VAT_SPECIAL", "增值税专票"),

    /**
     * 增值税普票
     */
    VAT_NORMAL("VAT_NORMAL", "增值税普票");

    private final String value;
    private final String label;

    public static boolean isValid(String kind) {
        if (kind == null) {
            return false;
        }
        for (InvoiceKindEnum e : values()) {
            if (e.getValue().equals(kind)) {
                return true;
            }
        }
        return false;
    }

}

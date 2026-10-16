package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收款单类型枚举（字典 lunaerp_finance_receipt_type）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ReceiptType {

    /**
     * 货款
     */
    RECEIPT("RECEIPT", "货款"),

    /**
     * 预收
     */
    ADVANCE("ADVANCE", "预收");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

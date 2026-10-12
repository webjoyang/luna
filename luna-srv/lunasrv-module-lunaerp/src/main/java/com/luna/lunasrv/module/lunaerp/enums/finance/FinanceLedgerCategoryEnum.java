package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 经营分录类别枚举
 * 对应字典：lunaerp_finance_ledger_category（2274，一期全集 9 值）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinanceLedgerCategoryEnum {

    /**
     * 发货应收成立
     */
    AR_INC("AR_INC", "发货应收"),

    /**
     * 发货应收红冲
     */
    AR_INC_REVERSAL("AR_INC_REVERSAL", "发货应收红冲"),

    /**
     * 收款入账
     */
    RECEIPT("RECEIPT", "收款入账"),

    /**
     * 收款红冲
     */
    RECEIPT_REVERSAL("RECEIPT_REVERSAL", "收款红冲"),

    /**
     * 收款核销
     */
    RECEIPT_WRITE_OFF("RECEIPT_WRITE_OFF", "收款核销"),

    /**
     * 预收转核销
     */
    ADVANCE_WRITE_OFF("ADVANCE_WRITE_OFF", "预收转核销"),

    /**
     * 退货冲减
     */
    AR_RETURN_REVERSAL("AR_RETURN_REVERSAL", "退货冲减"),

    /**
     * 供应商付款
     */
    OUT_SUPPLIER("OUT_SUPPLIER", "供应商付款"),

    /**
     * 供应商付款红冲
     */
    OUT_SUPPLIER_REVERSAL("OUT_SUPPLIER_REVERSAL", "供应商付款红冲");

    private final String value;
    private final String label;

    public static boolean isValid(String category) {
        if (category == null) {
            return false;
        }
        for (FinanceLedgerCategoryEnum e : values()) {
            if (e.getValue().equals(category)) {
                return true;
            }
        }
        return false;
    }

}

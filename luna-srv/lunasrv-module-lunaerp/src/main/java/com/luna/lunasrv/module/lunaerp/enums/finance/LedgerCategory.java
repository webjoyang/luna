package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 经营分录账类别枚举（字典 lunaerp_finance_ledger_category，一期全集9值）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum LedgerCategory {

    /**
     * 应收成立
     */
    AR_INC("AR_INC", "应收成立"),

    /**
     * 发货红冲应收冲减
     */
    AR_INC_REVERSAL("AR_INC_REVERSAL", "发货红冲应收冲减"),

    /**
     * 收款入账
     */
    RECEIPT("RECEIPT", "收款入账"),

    /**
     * 预收入账
     */
    ADVANCE("ADVANCE", "预收入账"),

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
     * 退货应收冲减
     */
    AR_RETURN_REVERSAL("AR_RETURN_REVERSAL", "退货应收冲减"),

    /**
     * 供应商付款入账
     */
    OUT_SUPPLIER("OUT_SUPPLIER", "供应商付款入账"),

    /**
     * 供应商付款红冲
     */
    OUT_SUPPLIER_REVERSAL("OUT_SUPPLIER_REVERSAL", "供应商付款红冲");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收付款核销业务类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentRefType {

    /**
     * 付款申请（OUT 侧）
     */
    PAY_APPLY("PAY_APPLY", "付款申请"),

    /**
     * 应收分录（IN 侧，10 号使用）
     */
    AR_ENTRY("AR_ENTRY", "应收分录");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

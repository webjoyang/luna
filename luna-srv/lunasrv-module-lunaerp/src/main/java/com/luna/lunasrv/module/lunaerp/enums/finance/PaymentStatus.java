package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收付款单状态枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {

    /**
     * 登记完成
     */
    COMPLETED("COMPLETED", "登记完成"),

    /**
     * 红冲取消（二期预留）
     */
    REVERSED("REVERSED", "红冲取消");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

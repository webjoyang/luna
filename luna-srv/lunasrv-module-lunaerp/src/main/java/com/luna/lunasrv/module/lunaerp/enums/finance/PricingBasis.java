package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计价基准枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PricingBasis {

    /**
     * 按米（梭织织费缺省基准）
     */
    METER("METER", "按米"),

    /**
     * 按公斤（染费缺省基准）
     */
    KG("KG", "按公斤");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

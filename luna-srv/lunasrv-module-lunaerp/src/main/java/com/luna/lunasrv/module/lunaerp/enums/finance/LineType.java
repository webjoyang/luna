package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商对账单行类型枚举（后端枚举，不入字典）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum LineType {

    /**
     * 加工费（织费/染费，来自检验单汇总）
     */
    PROCESS("PROCESS", "加工费"),

    /**
     * 采购货款（来自入库明细）
     */
    PURCHASE("PURCHASE", "采购货款"),

    /**
     * 调整项（正数运费补差/负数质量扣款等）
     */
    ADJUST("ADJUST", "调整项");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

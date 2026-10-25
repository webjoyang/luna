package com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打色指示单确认类型枚举（内部枚举，不入字典）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ColorInstructionConfirmTypeEnum {

    /**
     * 客户确认
     */
    CUSTOMER("CUSTOMER", "客户确认"),

    /**
     * 内部确认
     */
    INTERNAL("INTERNAL", "内部确认");

    private final String value;
    private final String label;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ColorInstructionConfirmTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产管理 - 打色指示单确认类型枚举（内部枚举，不入字典表）
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

    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (ColorInstructionConfirmTypeEnum e : values()) {
            if (e.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

}

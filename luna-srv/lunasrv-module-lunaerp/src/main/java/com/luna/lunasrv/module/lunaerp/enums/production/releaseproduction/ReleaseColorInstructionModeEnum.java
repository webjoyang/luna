package com.luna.lunasrv.module.lunaerp.enums.production.releaseproduction;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 下达生产 - 打色指示单模式枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ReleaseColorInstructionModeEnum {

    /**
     * 选已有打色单（须 CONFIRMED/IN_PRODUCTION）
     */
    EXISTING("EXISTING", "选已有"),

    /**
     * 新建打色单（生成 PENDING 单）
     */
    NEW("NEW", "新建"),

    /**
     * 不关联打色单
     */
    NONE("NONE", "不关联");

    private final String value;
    private final String label;

    public static ReleaseColorInstructionModeEnum of(String value) {
        if (value == null) {
            return NONE;
        }
        for (ReleaseColorInstructionModeEnum mode : values()) {
            if (mode.getValue().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return NONE;
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ReleaseColorInstructionModeEnum mode : values()) {
            if (mode.getValue().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

}

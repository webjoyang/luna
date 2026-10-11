package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 供应商对账单行类型枚举
 * 后端枚举，不入字典
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SupplierReconLineTypeEnum {

    /**
     * 加工费
     */
    PROCESS("PROCESS", "加工费"),

    /**
     * 采购货款
     */
    PURCHASE("PURCHASE", "采购货款"),

    /**
     * 调整项
     */
    ADJUST("ADJUST", "调整项");

    private final String value;
    private final String label;

    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (SupplierReconLineTypeEnum e : values()) {
            if (e.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

}

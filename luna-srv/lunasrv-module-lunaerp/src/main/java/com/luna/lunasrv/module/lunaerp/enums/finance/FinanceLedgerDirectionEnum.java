package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 经营分录借贷方向枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinanceLedgerDirectionEnum {

    /**
     * 借方（往来增加）
     */
    DEBIT("DEBIT", "借方（往来增加）"),

    /**
     * 贷方（往来减少）
     */
    CREDIT("CREDIT", "贷方（往来减少）");

    private final String value;
    private final String label;

    public static boolean isValid(String direction) {
        if (direction == null) {
            return false;
        }
        for (FinanceLedgerDirectionEnum e : values()) {
            if (e.getValue().equals(direction)) {
                return true;
            }
        }
        return false;
    }

}

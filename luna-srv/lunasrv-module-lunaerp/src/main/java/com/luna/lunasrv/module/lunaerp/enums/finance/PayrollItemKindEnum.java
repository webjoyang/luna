package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 工资款项大类与类型常量
 *
 * EARNING 字典 2285
 * DEDUCTION 字典 2286
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PayrollItemKindEnum {

    EARNING("EARNING", "收入"),
    DEDUCTION("DEDUCTION", "扣款");

    private final String value;
    private final String name;

    /**
     * 2285 收入款项有效字典项
     */
    public static final Set<String> VALID_EARNING_TYPES = Set.of(
            "BASE", "PIECE", "COMMISSION", "BONUS", "SUBSIDY", "OTHER"
    );

    /**
     * 2286 扣款款项手工录入有效字典项（DEFECT 次品扣款仅导入生成，07 票）
     */
    public static final Set<String> VALID_MANUAL_DEDUCTION_TYPES = Set.of(
            "OTHER"
    );

    /**
     * 2286 扣款款项全量字典项（含次品扣款）
     */
    public static final Set<String> ALL_DEDUCTION_TYPES = Set.of(
            "DEFECT", "OTHER"
    );

}

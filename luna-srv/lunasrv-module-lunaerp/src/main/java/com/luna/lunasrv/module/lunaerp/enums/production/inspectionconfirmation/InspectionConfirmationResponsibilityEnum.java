package com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质检责任类型枚举
 * 对应字典：lunaerp_inspection_responsibility
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InspectionConfirmationResponsibilityEnum {

    /**
     * 设备故障（免扣）
     */
    EQUIPMENT("EQUIPMENT", "设备故障（免扣）"),

    /**
     * 操作失误（扣款）
     */
    OPERATOR("OPERATOR", "操作失误（扣款）"),

    /**
     * 免扣
     */
    WAIVED("WAIVED", "免扣");

    private final String value;
    private final String label;

}

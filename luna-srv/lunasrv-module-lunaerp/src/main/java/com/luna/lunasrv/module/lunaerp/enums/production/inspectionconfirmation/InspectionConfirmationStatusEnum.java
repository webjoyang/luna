package com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质检确认状态枚举
 * 对应字典：lunaerp_inspection_confirmation_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InspectionConfirmationStatusEnum {

    /**
     * 待确认
     */
    PENDING("PENDING", "待确认"),

    /**
     * 已认定
     */
    CONFIRMED("CONFIRMED", "已认定"),

    /**
     * 已撤销
     */
    REVOKED("REVOKED", "已撤销");

    private final String value;
    private final String label;

}

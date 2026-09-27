package com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质检确认来源枚举（内部枚举，不入字典）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InspectionConfirmationSourceEnum {

    /**
     * 检验单引入
     */
    INSPECTION("INSPECTION", "检验单引入"),

    /**
     * 手工登记
     */
    MANUAL("MANUAL", "手工登记");

    private final String value;
    private final String label;

}

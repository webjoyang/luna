package com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打色指示单颜色行状态枚举
 * 对应字典：lunaerp_color_line_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ColorInstructionLineStatusEnum {

    /**
     * 待结果
     */
    PENDING("PENDING", "待结果"),

    /**
     * 通过
     */
    APPROVED("APPROVED", "通过"),

    /**
     * 驳回
     */
    REJECTED("REJECTED", "驳回");

    private final String value;
    private final String label;

}

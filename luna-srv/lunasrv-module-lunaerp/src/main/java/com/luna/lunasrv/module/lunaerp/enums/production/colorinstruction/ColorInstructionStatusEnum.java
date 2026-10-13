package com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 打色指示单状态枚举
 * 对应字典：lunaerp_color_instruction_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ColorInstructionStatusEnum {

    /**
     * 待打色
     */
    PENDING("PENDING", "待打色"),

    /**
     * 打色中
     */
    IN_PROGRESS("IN_PROGRESS", "打色中"),

    /**
     * 已确认
     */
    CONFIRMED("CONFIRMED", "已确认"),

    /**
     * 已投产
     */
    IN_PRODUCTION("IN_PRODUCTION", "已投产"),

    /**
     * 已作废
     */
    VOID("VOID", "已作废");

    private final String value;
    private final String label;

}

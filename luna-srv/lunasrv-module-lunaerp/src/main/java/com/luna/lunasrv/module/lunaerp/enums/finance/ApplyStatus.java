package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务单据审批状态枚举（对齐 02 号审批引擎五态）
 * 对应字典：lunaerp_approval_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ApplyStatus {

    /**
     * 待提交 / 草稿
     */
    PENDING("PENDING", "待提交"),

    /**
     * 审批中
     */
    APPROVING("APPROVING", "审批中"),

    /**
     * 已通过
     */
    APPROVED("APPROVED", "已通过"),

    /**
     * 已驳回
     */
    REJECTED("REJECTED", "已驳回"),

    /**
     * 已撤回
     */
    CANCELLED("CANCELLED", "已撤回");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

    public static boolean isDraft(String status) {
        return PENDING.name().equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status);
    }

}

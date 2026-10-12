package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 发票申请状态枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum InvoiceApplicationStatusEnum {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已提交
     */
    SUBMITTED("SUBMITTED", "已提交"),

    /**
     * 已开票
     */
    ISSUED("ISSUED", "已开票"),

    /**
     * 已驳回
     */
    REJECTED("REJECTED", "已驳回");

    private final String value;
    private final String label;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (InvoiceApplicationStatusEnum e : values()) {
            if (e.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

}

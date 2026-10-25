package com.luna.lunasrv.module.lunaerp.enums.business.salesreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务管理 - 销售退货原因枚举（行级必填）
 * 对应字典：lunaerp_sales_return_reason（id=2280）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SalesReturnReasonEnum {

    /**
     * 质量问题
     */
    QUALITY("QUALITY", "质量问题"),

    /**
     * 发错货
     */
    WRONG("WRONG", "发错货"),

    /**
     * 多发
     */
    EXCESS("EXCESS", "多发"),

    /**
     * 客户原因
     */
    CUSTOMER("CUSTOMER", "客户原因"),

    /**
     * 其他
     */
    OTHER("OTHER", "其他");

    private final String value;
    private final String label;

    public static boolean isValid(String reason) {
        if (reason == null) {
            return false;
        }
        for (SalesReturnReasonEnum e : values()) {
            if (e.getValue().equals(reason)) {
                return true;
            }
        }
        return false;
    }

}

package com.luna.lunasrv.module.lunaerp.enums.business.salesreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务管理 - 销售退货业务状态枚举
 * 对应字典：lunaerp_sales_return_status（id=2279）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SalesReturnStatusEnum {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已审批
     */
    APPROVED("APPROVED", "已审批"),

    /**
     * 已收货
     */
    RECEIVED("RECEIVED", "已收货"),

    /**
     * 已过账
     */
    POSTED("POSTED", "已过账"),

    /**
     * 已作废
     */
    CANCELLED("CANCELLED", "已作废");

    private final String value;
    private final String label;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (SalesReturnStatusEnum e : values()) {
            if (e.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

}

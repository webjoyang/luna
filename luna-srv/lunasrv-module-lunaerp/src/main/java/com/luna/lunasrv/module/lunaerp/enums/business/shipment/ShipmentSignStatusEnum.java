package com.luna.lunasrv.module.lunaerp.enums.business.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务管理 - 发货单签收状态枚举
 * 对应字典：lunaerp_shipment_sign_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ShipmentSignStatusEnum {

    /**
     * 待签收
     */
    PENDING("PENDING", "待签收"),

    /**
     * 已签收
     */
    SIGNED("SIGNED", "已签收"),

    /**
     * 拒签
     */
    REFUSED("REFUSED", "拒签");

    private final String value;
    private final String label;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (ShipmentSignStatusEnum e : values()) {
            if (e.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

}

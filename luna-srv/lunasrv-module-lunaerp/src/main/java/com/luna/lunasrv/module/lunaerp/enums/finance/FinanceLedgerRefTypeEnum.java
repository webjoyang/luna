package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 经营分录业务源单类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum FinanceLedgerRefTypeEnum {

    /**
     * 发货单
     */
    SHIPMENT("SHIPMENT", "发货单"),

    /**
     * 发货单明细行
     */
    SHIPMENT_ITEM("SHIPMENT_ITEM", "发货明细"),

    /**
     * 发货运费
     */
    SHIPMENT_FREIGHT("SHIPMENT_FREIGHT", "发货运费"),

    /**
     * 收付款单
     */
    PAYMENT("PAYMENT", "收付款单"),

    /**
     * 销售退货单
     */
    RETURN_ORDER("RETURN_ORDER", "销售退货单");

    private final String value;
    private final String label;

    public static boolean isValid(String refType) {
        if (refType == null) {
            return false;
        }
        for (FinanceLedgerRefTypeEnum e : values()) {
            if (e.getValue().equals(refType)) {
                return true;
            }
        }
        return false;
    }

}

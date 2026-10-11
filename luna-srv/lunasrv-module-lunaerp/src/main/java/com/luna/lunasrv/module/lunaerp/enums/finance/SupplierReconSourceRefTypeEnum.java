package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财务管理 - 供应商对账单源单据类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SupplierReconSourceRefTypeEnum {

    /**
     * 检验单
     */
    INSPECTION("INSPECTION", "检验单"),

    /**
     * 入库明细
     */
    STOCK_IN_ITEM("STOCK_IN_ITEM", "入库明细");

    private final String value;
    private final String label;

    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (SupplierReconSourceRefTypeEnum e : values()) {
            if (e.getValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

}

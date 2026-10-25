package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账单行来源单据类型枚举
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SourceRefType {

    /**
     * 检验单汇总（加工费）
     */
    INSPECTION("INSPECTION", "检验单"),

    /**
     * 入库明细（采购货款）
     */
    STOCK_IN_ITEM("STOCK_IN_ITEM", "入库明细");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

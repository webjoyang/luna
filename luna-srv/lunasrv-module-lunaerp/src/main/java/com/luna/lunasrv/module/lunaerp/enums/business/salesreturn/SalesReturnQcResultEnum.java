package com.luna.lunasrv.module.lunaerp.enums.business.salesreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务管理 - 销售退货质检判定枚举（逐卷判定）
 * 对应字典：lunaerp_sales_return_qc_result（id=2281）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum SalesReturnQcResultEnum {

    /**
     * 可二次入库（进入入库单，审核写入库存台账 STOCK_IN 正流水）
     */
    RESTOCK("RESTOCK", "可二次入库"),

    /**
     * 废品（不写库存台账，仅留质检记录）
     */
    SCRAP("SCRAP", "废品");

    private final String value;
    private final String label;

    public static boolean isValid(String result) {
        if (result == null) {
            return false;
        }
        for (SalesReturnQcResultEnum e : values()) {
            if (e.getValue().equals(result)) {
                return true;
            }
        }
        return false;
    }

}

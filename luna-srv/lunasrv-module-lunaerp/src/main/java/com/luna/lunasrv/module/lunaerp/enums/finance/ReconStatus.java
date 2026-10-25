package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商对账单状态枚举
 * 对应字典：lunaerp_supplier_recon_status
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ReconStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已确认
     */
    CONFIRMED("CONFIRMED", "已确认"),

    /**
     * 已作废
     */
    CANCELLED("CANCELLED", "已作废");

    private final String value;
    private final String label;

    public String getCode() {
        return value;
    }

}

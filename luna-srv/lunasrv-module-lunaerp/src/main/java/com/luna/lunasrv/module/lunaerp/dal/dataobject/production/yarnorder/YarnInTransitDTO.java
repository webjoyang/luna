package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 纱线在途采购量聚合 DTO（ADR-0041）
 *
 * @author luna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YarnInTransitDTO {

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 在途采购量（SUM(purchase_qty)）
     */
    private BigDecimal inTransitQty;

}

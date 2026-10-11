package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 纱线规格库存余额聚合 DTO（ADR-0017 / ADR-0041）
 *
 * @author luna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YarnStockBalanceDTO {

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 实时可用余额（SUM(quantity)）
     */
    private BigDecimal balance;

}

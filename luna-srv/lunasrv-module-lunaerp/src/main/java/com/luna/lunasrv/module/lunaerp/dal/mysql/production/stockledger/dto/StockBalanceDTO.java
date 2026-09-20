package com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 纱线规格库存聚合 DTO
 *
 * @author luna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceDTO {

    /**
     * 纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 聚合余额
     */
    private BigDecimal balance;

}

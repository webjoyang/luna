package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户收支方向金额统计 DTO
 *
 * @author luna
 */
@Data
public class AccountDirectionAmountDTO {

    /**
     * 资金账户ID
     */
    private Long accountId;

    /**
     * 收支方向（DEBIT 流入 / CREDIT 流出）
     */
    private String direction;

    /**
     * 累计金额
     */
    private BigDecimal totalAmount;

}

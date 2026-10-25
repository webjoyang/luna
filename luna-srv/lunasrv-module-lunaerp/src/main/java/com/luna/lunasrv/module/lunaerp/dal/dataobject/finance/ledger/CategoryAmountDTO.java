package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 经营分录类别发生额统计 DTO
 *
 * @author luna
 */
@Data
public class CategoryAmountDTO {

    /**
     * 分录类别
     */
    private String category;

    /**
     * 收支方向（DEBIT / CREDIT）
     */
    private String direction;

    /**
     * 原币发生额
     */
    private BigDecimal originalAmount;

    /**
     * 本位币发生额
     */
    private BigDecimal baseAmount;

    /**
     * 笔数
     */
    private Long count;

}

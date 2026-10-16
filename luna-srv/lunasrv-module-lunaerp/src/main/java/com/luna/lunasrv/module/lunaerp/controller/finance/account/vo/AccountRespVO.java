package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务管理 - 资金账户 Response VO
 *
 * @author luna
 */
@Data
public class AccountRespVO {

    /**
     * 资金账户ID
     */
    private Long id;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 账户类型
     */
    private String accountType;

    /**
     * 账户币种
     */
    private String currency;

    /**
     * 期初余额
     */
    private BigDecimal openingBalance;

    /**
     * 当前余额
     */
    private BigDecimal currentBalance;

    /**
     * 状态（ENABLE/DISABLE）
     */
    private String status;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

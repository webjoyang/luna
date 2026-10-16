package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户创建 Request VO
 *
 * @author luna
 */
@Data
public class AccountSaveReqVO {

    /**
     * 账户名称
     */
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    /**
     * 账户类型（CASH/BANK/WECHAT/ALIPAY）
     */
    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    /**
     * 账户币种（默认 RMB）
     */
    private String currency;

    /**
     * 期初余额（仅创建时录入一次，此后只读）
     */
    @NotNull(message = "期初余额不能为空")
    private BigDecimal openingBalance;

    /**
     * 备注说明
     */
    private String remark;

}

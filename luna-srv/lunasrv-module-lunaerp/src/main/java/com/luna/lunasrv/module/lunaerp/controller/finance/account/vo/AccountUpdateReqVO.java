package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户修改 Request VO
 *
 * 可改字段：备注/状态（停用↔启用）/account_no/account_nature；
 * opening_balance 仅无分录可改（020_104）；
 * account_name 查重租户内唯一含软删行（020_102）；
 * 币种与类型建后不可改。
 *
 * @author luna
 */
@Data
public class AccountUpdateReqVO {

    /**
     * 资金账户ID
     */
    @NotNull(message = "资金账户ID不能为空")
    private Long id;

    /**
     * 账户名称（租户内全量唯一含软删行）
     */
    private String accountName;

    /**
     * 账号/微信标识/支付宝标识（BANK/WECHAT/ALIPAY 必填，CASH 可空）
     */
    private String accountNo;

    /**
     * 账户性质（PUBLIC 公户 / PRIVATE 私户）
     */
    private String accountNature;

    /**
     * 期初余额（仅无分录时允许修改，否则 020_104）
     */
    private BigDecimal openingBalance;

    /**
     * 状态（ENABLE 启用 / DISABLE 停用）
     */
    @Pattern(regexp = "^(ENABLE|DISABLE)$", message = "账户状态必须为 ENABLE 或 DISABLE")
    private String status;

    /**
     * 备注说明（可清空）
     */
    private String remark;

}

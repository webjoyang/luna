package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
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
     * 账号/微信标识/支付宝标识（@Sensitive(CONTACT)）
     */
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.BANK_CARD)
    private String accountNo;

    /**
     * 账户性质（PUBLIC 公户 / PRIVATE 私户）
     */
    private String accountNature;

    /**
     * 账户币种
     */
    private String currency;

    /**
     * 期初余额
     */
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal openingBalance;

    /**
     * 当前余额
     */
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
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

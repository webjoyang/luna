package com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户收支汇总行 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户收支汇总行 Response VO")
@Data
public class AccountSummaryRespVO {

    @Schema(description = "资金账户ID（合计行为空）", example = "10")
    private Long accountId;

    @Schema(description = "账户名称", example = "基本户（工行）")
    private String accountName;

    @Schema(description = "账户类型（BANK / CASH / ALIPAY / WECHAT）", example = "BANK")
    private String accountType;

    @Schema(description = "账户性质（CORPORATE 对公 / PERSONAL 对私）", example = "CORPORATE")
    private String accountNature;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "期初余额", example = "100000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal openingBalance;

    @Schema(description = "本期流入（借方发生额）", example = "50000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal inflowAmount;

    @Schema(description = "本期流出（贷方发生额）", example = "20000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal outflowAmount;

    @Schema(description = "期末余额（期初 + 流入 - 流出）", example = "130000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal endingBalance;

    @Schema(description = "当前可用余额（系统物化缓存）", example = "130000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal currentBalance;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 资金账户精简下拉 Response VO")
@Data
public class FinanceAccountSimpleRespVO {

    @Schema(description = "账户ID", example = "3001")
    private Long id;

    @Schema(description = "账户名称", example = "招商银行一般户")
    private String accountName;

    @Schema(description = "账户类型（CASH/BANK/WECHAT/ALIPAY）", example = "BANK")
    private String accountType;

    @Schema(description = "账户币种", example = "RMB")
    private String currency;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

    @Schema(description = "当前余额（元）", example = "100000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal currentBalance;

}

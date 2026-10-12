package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户精简响应 VO（下拉选单）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户精简列表 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSimpleRespVO {

    @Schema(description = "资金账户ID", example = "1024")
    private Long id;

    @Schema(description = "资金账户名称", example = "招商银行主基本户")
    private String accountName;

    @Schema(description = "账户类型（CASH/BANK/WECHAT/ALIPAY）", example = "BANK")
    private String accountType;

    @Schema(description = "账户币种", example = "CNY")
    private String currency;

    @Schema(description = "当前余额", example = "25000.00")
    private BigDecimal currentBalance;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

}

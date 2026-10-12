package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务管理 - 资金账户响应 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRespVO {

    @Schema(description = "资金账户ID", example = "1024")
    private Long id;

    @Schema(description = "资金账户名称", example = "招商银行主基本户")
    private String accountName;

    @Schema(description = "账户类型（CASH/BANK/WECHAT/ALIPAY）", example = "BANK")
    private String accountType;

    @Schema(description = "账户币种", example = "CNY")
    private String currency;

    @Schema(description = "期初余额", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "当前余额", example = "25000.00")
    private BigDecimal currentBalance;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

    @Schema(description = "备注说明", example = "基本户")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

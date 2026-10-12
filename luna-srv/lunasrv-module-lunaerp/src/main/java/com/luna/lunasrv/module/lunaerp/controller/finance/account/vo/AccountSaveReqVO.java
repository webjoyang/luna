package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户保存请求 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户保存 Request VO")
@Data
public class AccountSaveReqVO {

    @Schema(description = "资金账户ID", example = "1024")
    private Long id;

    @Schema(description = "资金账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "招商银行主基本户")
    @NotBlank(message = "资金账户名称不能为空")
    private String accountName;

    @Schema(description = "账户类型（CASH/BANK/WECHAT/ALIPAY）", requiredMode = Schema.RequiredMode.REQUIRED, example = "BANK")
    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    @Schema(description = "账户币种", example = "CNY")
    private String currency;

    @Schema(description = "期初余额（仅创建时可录入，后续只读）", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

    @Schema(description = "备注说明", example = "基本户")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 自动分配核销试算 Request VO")
@Data
public class AllocatePreviewReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "币种（严格同币种核销）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RMB")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "本次收款金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额必须大于0")
    private BigDecimal payAmount;

    @Schema(description = "收款类型（RECEIPT / ADVANCE）", example = "RECEIPT")
    private String receiptType;

}

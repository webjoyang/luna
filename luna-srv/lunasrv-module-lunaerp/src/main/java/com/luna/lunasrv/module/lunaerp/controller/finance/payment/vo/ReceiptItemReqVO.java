package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 收款单核销明细 Request VO")
@Data
public class ReceiptItemReqVO {

    @Schema(description = "关联的经营分录账ID（AR_INC 分录ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "分录ID不能为空")
    private Long entryId;

    @Schema(description = "本次核销分配金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1500.00")
    @NotNull(message = "核销分配金额不能为空")
    @DecimalMin(value = "0.01", message = "核销分配金额必须大于0")
    private BigDecimal allocateAmount;

}

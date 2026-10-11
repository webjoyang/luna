package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 付款登记明细 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRegisterItemReqVO {

    @Schema(description = "付款申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "付款申请ID不能为空")
    private Long applyId;

    @Schema(description = "本次核销分配金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "核销分配金额不能为空")
    @DecimalMin(value = "0.01", message = "核销分配金额必须大于0")
    private BigDecimal allocateAmount;

}

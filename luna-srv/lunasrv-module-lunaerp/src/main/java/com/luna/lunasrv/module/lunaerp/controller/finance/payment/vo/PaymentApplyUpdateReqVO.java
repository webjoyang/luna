package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 付款申请单修改 Request VO")
@Data
public class PaymentApplyUpdateReqVO {

    @Schema(description = "付款申请单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "付款申请单ID不能为空")
    private Long id;

    @Schema(description = "本次申请付款金额（元，必须 >0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6000.00")
    @NotNull(message = "申请付款金额不能为空")
    @DecimalMin(value = "0.01", message = "申请付款金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "预付用途备注（PREPAY 时必填）", example = "采购纱线定金修改")
    private String purpose;

    @Schema(description = "备注", example = "调整金额重提")
    private String remark;

}

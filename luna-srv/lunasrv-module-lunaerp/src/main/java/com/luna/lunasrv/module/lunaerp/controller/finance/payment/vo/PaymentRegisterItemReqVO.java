package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 付款登记核销明细 Request VO")
@Data
public class PaymentRegisterItemReqVO {

    @Schema(description = "关联付款申请单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "付款申请单ID不能为空")
    private Long applyId;

    @Schema(description = "本次核销分配金额（元，可空表示由系统按申请创建顺序自动分配）", example = "1000.00")
    private BigDecimal allocateAmount;

}

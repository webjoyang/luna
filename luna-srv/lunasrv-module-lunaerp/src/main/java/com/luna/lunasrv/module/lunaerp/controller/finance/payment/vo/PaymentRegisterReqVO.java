package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 付款登记 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRegisterReqVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @Schema(description = "本次实付金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "本次实付金额不能为空")
    @DecimalMin(value = "0", message = "本次实付金额不能小于0")
    private BigDecimal payAmount;

    @Schema(description = "预付抵扣金额（元，缺省0）", example = "2000.00")
    @DecimalMin(value = "0", message = "预付抵扣金额不能小于0")
    private BigDecimal prepayUsedAmount;

    @Schema(description = "备注", example = "第一批次付款")
    private String remark;

    @Schema(description = "核销明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "核销明细不能为空")
    @Valid
    private List<PaymentRegisterItemReqVO> items;

}

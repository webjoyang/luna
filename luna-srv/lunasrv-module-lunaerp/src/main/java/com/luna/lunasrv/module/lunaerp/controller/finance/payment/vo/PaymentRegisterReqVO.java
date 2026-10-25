package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 付款登记 Request VO（OUT 侧）")
@Data
public class PaymentRegisterReqVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @Schema(description = "本次资金实付金额（元，缺省 0）", example = "5000.00")
    private BigDecimal payAmount;

    @Schema(description = "预付抵扣金额（元，缺省 0）", example = "1000.00")
    private BigDecimal prepayUsedAmount;

    @Schema(description = "备注", example = "10月第一笔货款结算")
    private String remark;

    @Schema(description = "核销申请明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "核销申请明细不能为空")
    @Valid
    private List<PaymentRegisterItemReqVO> items;

}

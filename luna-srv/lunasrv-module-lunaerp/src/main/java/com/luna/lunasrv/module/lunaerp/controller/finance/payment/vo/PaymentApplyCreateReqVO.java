package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 付款申请单创建 Request VO")
@Data
public class PaymentApplyCreateReqVO {

    @Schema(description = "申请类型（RECON 对账付款 / PREPAY 预付付款）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RECON")
    @NotBlank(message = "申请类型不能为空")
    private String applyType;

    @Schema(description = "供应商ID（PREPAY 时必填）", example = "1001")
    private Long supplierId;

    @Schema(description = "关联合同对账单ID（RECON 时必填）", example = "2001")
    private Long reconId;

    @Schema(description = "本次申请付款金额（元，必须 >0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "申请付款金额不能为空")
    @DecimalMin(value = "0.01", message = "申请付款金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "预付用途备注（PREPAY 时必填，如纱线采购定金）", example = "采购纱线定金")
    private String purpose;

    @Schema(description = "收款银行名称快照", example = "中国工商银行")
    private String bankName;

    @Schema(description = "收款银行账号快照", example = "622202100011223344")
    private String bankAccount;

    @Schema(description = "备注", example = "申请加急付款")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 付款申请保存（创建/更新） Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApplySaveReqVO {

    @Schema(description = "付款申请ID（更新时必填）", example = "100")
    private Long id;

    @Schema(description = "申请类型（RECON 对账付款 / PREPAY 预付付款）", example = "RECON")
    private String applyType;

    @Schema(description = "供应商ID（PREPAY 类型必填）", example = "1001")
    private Long supplierId;

    @Schema(description = "关联对账单ID（RECON 类型必填）", example = "200")
    private Long reconId;

    @Schema(description = "本次申请付款金额（元，必须 >0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "申请金额不能为空")
    private BigDecimal amount;

    @Schema(description = "付款用途（PREPAY 类型必填）", example = "纱线采购定金")
    private String purpose;

    @Schema(description = "开户行名称（可选覆盖快照）", example = "中国工商银行")
    private String bankName;

    @Schema(description = "银行账号（可选覆盖快照）", example = "6222021000000000")
    private String bankAccount;

    @Schema(description = "备注", example = "第一批付款申请")
    private String remark;

}

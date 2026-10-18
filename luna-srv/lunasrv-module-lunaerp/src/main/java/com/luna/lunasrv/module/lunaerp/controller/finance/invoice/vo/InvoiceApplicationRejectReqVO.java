package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 发票申请驳回 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceApplicationRejectReqVO {

    @Schema(description = "发票申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "发票申请ID不能为空")
    private Long id;

    @Schema(description = "驳回原因说明", example = "客户税号错误，需重新确认")
    private String rejectReason;

}

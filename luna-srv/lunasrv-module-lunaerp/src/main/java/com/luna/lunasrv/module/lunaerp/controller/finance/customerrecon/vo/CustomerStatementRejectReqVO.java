package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单异议驳回 Request VO")
@Data
public class CustomerStatementRejectReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "异议驳回原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "第3笔货款数量有误")
    @NotBlank(message = "异议原因不能为空")
    private String rejectReason;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单驳回/异议 Request VO")
@Data
public class CustomerStatementRejectReqVO {

    @Schema(description = "对账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "对账单编号不能为空")
    private Long id;

    @Schema(description = "驳回异议原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "单价与合同不符")
    @NotBlank(message = "驳回异议原因不能为空")
    private String rejectReason;

}

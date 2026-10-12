package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "客户门户 - 对账单驳回请求 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalStatementRejectReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "驳回原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "本期发货金额与实际不符")
    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;

    @Schema(description = "显式客户ID（可选，内部代客场景）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1001")
    private Long customerId;

}

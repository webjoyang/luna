package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "客户门户 - 对账单确认请求 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalStatementConfirmReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "确认人姓名（手填姓名必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "确认人姓名不能为空")
    private String confirmByName;

    @Schema(description = "显式客户ID（可选，内部代客场景）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1001")
    private Long customerId;

}

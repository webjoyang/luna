package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "客户门户 - 客户对账单在线确认 Request VO")
@Data
public class CustomerStatementPortalConfirmReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "客户ID（租户与客户隔离校验，代客场景必填，门户免登由 Token 注入）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2001")
    private Long customerId;

    @Schema(description = "确认人姓名（手填姓名必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张采购")
    @NotBlank(message = "确认人姓名不能为空")
    private String confirmByName;

    @Schema(description = "确认客户端IP（由 Controller 自动提取客户端真实IP，禁止前端伪造）", hidden = true)
    private String confirmIp;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单代客确认 Request VO")
@Data
public class CustomerStatementConfirmOnBehalfReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "代客确认线下方式（微信/电话/邮件/纸质签字等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "微信")
    @NotBlank(message = "代客确认方式不能为空")
    private String confirmMethod;

    @Schema(description = "代客确认备注说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户李总微信确认无误")
    @NotBlank(message = "代客确认备注说明不能为空")
    private String remark;

}

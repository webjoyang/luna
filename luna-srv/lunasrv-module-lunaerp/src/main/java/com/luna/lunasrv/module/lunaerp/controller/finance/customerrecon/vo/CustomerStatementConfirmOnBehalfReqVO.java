package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单代客确认 Request VO")
@Data
public class CustomerStatementConfirmOnBehalfReqVO {

    @Schema(description = "对账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "对账单编号不能为空")
    private Long id;

    @Schema(description = "线下确认方式/凭证备注（如：邮件确认/微信确认/线下盖章纸质件）", requiredMode = Schema.RequiredMode.REQUIRED, example = "微信确认截图已归档")
    @NotBlank(message = "线下确认方式不能为空")
    private String confirmMethod;

    @Schema(description = "备注说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户业务对接人张三微信文字确认")
    @NotBlank(message = "代客确认线下备注不可为空")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 审批实例提交 Request VO")
@Data
public class ApprovalInstanceSubmitReqVO {

    @Schema(description = "业务单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    @NotBlank(message = "业务单据类型不能为空")
    private String bizType;

    @Schema(description = "业务单据ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "业务单据ID不能为空")
    private Long bizId;

    @Schema(description = "提交说明/备注", example = "请领导审批")
    private String comment;

}

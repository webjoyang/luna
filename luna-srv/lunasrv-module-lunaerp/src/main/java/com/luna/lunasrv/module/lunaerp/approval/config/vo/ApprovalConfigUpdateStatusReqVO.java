package com.luna.lunasrv.module.lunaerp.approval.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 审批流程配置状态更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigUpdateStatusReqVO {

    @Schema(description = "状态（ENABLED 启用 / DISABLED 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENABLED")
    @NotBlank(message = "状态不能为空")
    private String status;

}

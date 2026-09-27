package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 检验单生成质检确认 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionConfirmationGenerateReqVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "检验单ID不能为空")
    private Long inspectionOrderId;

}

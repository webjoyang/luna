package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理-质检确认撤销重录 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionConfirmationRevokeReqVO {

    @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "质检确认记录ID不能为空")
    private Long id;

}

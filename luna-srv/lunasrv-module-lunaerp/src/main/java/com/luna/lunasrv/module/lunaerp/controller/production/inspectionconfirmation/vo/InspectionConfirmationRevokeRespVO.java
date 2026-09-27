package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理-质检确认撤销重录 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionConfirmationRevokeRespVO {

    @Schema(description = "已撤销冻结记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long revokedId;

    @Schema(description = "新克隆生成的待确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1025")
    private Long freshId;

}

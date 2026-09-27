package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 检验单生成质检确认 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionConfirmationGenerateRespVO {

    @Schema(description = "本次生成条数", example = "3")
    private Integer generatedCount;

    @Schema(description = "跳过条数", example = "5")
    private Integer skippedCount;

}

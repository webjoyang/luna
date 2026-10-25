package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理 - 排产甘特图 织机行 VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanttLoomVO {

    @Schema(description = "织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long loomId;

    @Schema(description = "机台号", requiredMode = Schema.RequiredMode.REQUIRED, example = "L-01")
    private String loomNo;

    @Schema(description = "型号", example = "喷气 230")
    private String model;

}

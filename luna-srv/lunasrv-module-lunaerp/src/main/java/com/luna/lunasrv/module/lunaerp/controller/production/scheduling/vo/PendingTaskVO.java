package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理 - 待排池无日期任务 Response VO")
@Data
public class PendingTaskVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "11")
    private Long taskId;

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MT-ZZ2026100701-02")
    private String taskCode;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Long weavingOrderId;

    @Schema(description = "关联织造单号快照", example = "WO2026100701")
    private String weavingOrderCode;

    @Schema(description = "织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long loomId;

    @Schema(description = "机台号快照", example = "L-01")
    private String loomNo;

    @Schema(description = "计划匹数", example = "10")
    private Integer plannedQty;

}

package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 生产管理 - 甘特图任务条带 Response VO")
@Data
public class GanttBarVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long taskId;

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MT-WO2026100701-01")
    private String taskCode;

    @Schema(description = "织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long loomId;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Long weavingOrderId;

    @Schema(description = "关联织造单号快照", example = "WO2026100701")
    private String weavingOrderCode;

    @Schema(description = "产品编号快照", example = "P001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉汗布")
    private String productName;

    @Schema(description = "计划匹数", example = "30")
    private Integer plannedQty;

    @Schema(description = "完工/实织匹数", example = "12")
    private Integer actualQty;

    @Schema(description = "挡车工姓名快照", example = "张三")
    private String workerName;

    @Schema(description = "计划开始日期", example = "2026-10-08")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期", example = "2026-10-12")
    private LocalDate plannedEnd;

    @Schema(description = "任务状态（PENDING/IN_PROGRESS/HALTED/COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "IN_PROGRESS")
    private String status;

    @Schema(description = "是否冲突（同织机计划时段与他任务重叠）", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean conflict;

}

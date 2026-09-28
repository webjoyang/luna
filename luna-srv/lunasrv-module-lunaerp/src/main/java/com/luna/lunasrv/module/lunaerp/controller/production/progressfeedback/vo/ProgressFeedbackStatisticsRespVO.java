package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理-进度反馈统计条 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackStatisticsRespVO {

    @Schema(description = "未排缸颜色行数（零缸节数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long unscheduledColorCount;

    @Schema(description = "进行中缸数（已排缸/染色中/固色中/质检中）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Long inProgressVatCount;

    @Schema(description = "已完成缸数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Long completedVatCount;

    @Schema(description = "延期缸数（染色单交货日期 < 今天 且 缸未完成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long delayedVatCount;

}

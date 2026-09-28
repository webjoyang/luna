package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-进度反馈记录 Response VO")
@Data
public class ProgressFeedbackNoteRespVO {

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long id;

    @Schema(description = "所属反馈缸ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long feedbackId;

    @Schema(description = "反馈沟通内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "染厂反馈坯布有轻微横档，已要求排查")
    private String content;

    @Schema(description = "是否标记为问题（true 是，false 否）", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean issueFlag;

    @Schema(description = "问题闭环时间（可空）", example = "2026-09-28 15:30:00")
    private LocalDateTime resolvedAt;

    @Schema(description = "记录人（创建者）", example = "admin")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-28 10:00:00")
    private LocalDateTime createTime;

}

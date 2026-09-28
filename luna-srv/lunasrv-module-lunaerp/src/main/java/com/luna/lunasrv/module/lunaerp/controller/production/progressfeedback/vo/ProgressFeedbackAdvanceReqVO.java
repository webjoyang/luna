package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生产管理 - 进度反馈工序推进 Request VO
 *
 * @author luna
 */
@Schema(description = "生产管理 - 进度反馈工序推进 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackAdvanceReqVO {

    @Schema(description = "反馈缸 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "反馈缸ID不能为空")
    private Long id;

    @Schema(description = "目标工序状态（可选；若传则严格校验为下一格工序，不允许跳格）", example = "DYEING")
    private String targetStatus;

}

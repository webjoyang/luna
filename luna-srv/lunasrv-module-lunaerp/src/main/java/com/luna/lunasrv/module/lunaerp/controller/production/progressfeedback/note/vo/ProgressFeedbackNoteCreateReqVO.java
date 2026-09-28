package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-进度反馈记录创建 Request VO")
@Data
public class ProgressFeedbackNoteCreateReqVO {

    @Schema(description = "所属反馈缸ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "所属反馈缸ID不能为空")
    private Long feedbackId;

    @Schema(description = "反馈沟通内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "染厂反馈坯布有轻微横档，已要求排查")
    @NotBlank(message = "反馈沟通内容不能为空")
    @Size(max = 1000, message = "反馈沟通内容长度不能超过1000个字符")
    private String content;

    @Schema(description = "是否标记为问题（true 是，false 否，缺省 false）", example = "true")
    private Boolean issueFlag;

}

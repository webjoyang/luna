package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理 - 进度反馈缸详情编辑 Request VO
 *
 * @author luna
 */
@Schema(description = "生产管理 - 进度反馈缸详情编辑 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackUpdateReqVO {

    @Schema(description = "反馈缸 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "反馈缸ID不能为空")
    private Long id;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VAT2026-001")
    @NotBlank(message = "缸号不能为空")
    @Size(max = 50, message = "缸号长度不能超过 50 个字符")
    private String vatNo;

    @Schema(description = "本缸数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @NotNull(message = "本缸数量不能为空")
    @DecimalMin(value = "0.0001", message = "本缸数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "工序状态（可空，传值时仅支持回退到上一工序）", example = "DYEING")
    private String status;

    @Schema(description = "排缸日期（可人工修正）", example = "2026-09-28")
    private LocalDate scheduledDate;

    @Schema(description = "染色开始日期（可人工修正）", example = "2026-09-29")
    private LocalDate dyeingStartedDate;

    @Schema(description = "固色开始日期（可人工修正）", example = "2026-09-30")
    private LocalDate fixingStartedDate;

    @Schema(description = "质检日期（可人工修正）", example = "2026-10-01")
    private LocalDate qualityCheckDate;

    @Schema(description = "完成日期（可人工修正）", example = "2026-10-02")
    private LocalDate completedDate;

    @Schema(description = "延期原因", example = "染厂设备检修")
    @Size(max = 1000, message = "延期原因长度不能超过 1000 个字符")
    private String delayReason;

    @Schema(description = "备注", example = "人工修正完成日期")
    @Size(max = 1000, message = "备注长度不能超过 1000 个字符")
    private String remark;

}

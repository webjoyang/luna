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
 * 生产管理 - 进度反馈建缸 Request VO
 *
 * @author luna
 */
@Schema(description = "生产管理 - 进度反馈建缸 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackCreateReqVO {

    @Schema(description = "染色单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "染色单不能为空")
    private Long dyeingOrderId;

    @Schema(description = "染色单颜色明细 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "染色颜色明细不能为空")
    private Long dyeingColorId;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "V20260901")
    @NotBlank(message = "缸号不能为空")
    @Size(max = 50, message = "缸号长度不能超过 50 个字符")
    private String vatNo;

    @Schema(description = "本缸数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @NotNull(message = "本缸数量不能为空")
    @DecimalMin(value = "0.0001", message = "本缸数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "10")
    private Integer pieces;

    @Schema(description = "排缸日期（可空，缺省当天）", example = "2026-09-28")
    private LocalDate scheduledDate;

    @Schema(description = "备注", example = "首缸急产")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}

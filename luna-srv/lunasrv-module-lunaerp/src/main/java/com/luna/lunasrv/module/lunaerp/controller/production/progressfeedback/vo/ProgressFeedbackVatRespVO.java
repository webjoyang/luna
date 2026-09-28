package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-进度反馈缸卡 Response VO")
@Data
public class ProgressFeedbackVatRespVO {

    @Schema(description = "反馈缸ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long dyeingOrderId;

    @Schema(description = "染色颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Long dyeingColorId;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VAT2026-001")
    private String vatNo;

    @Schema(description = "本缸数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "进度状态（SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCHEDULED")
    private String status;

    @Schema(description = "排缸日期", example = "2026-09-28")
    private LocalDate scheduledDate;

    @Schema(description = "染色开始日期", example = "2026-09-29")
    private LocalDate dyeingStartedDate;

    @Schema(description = "固色开始日期", example = "2026-09-30")
    private LocalDate fixingStartedDate;

    @Schema(description = "质检日期", example = "2026-10-01")
    private LocalDate qualityCheckDate;

    @Schema(description = "完成日期", example = "2026-10-02")
    private LocalDate completedDate;

    @Schema(description = "延期原因", example = "等原料")
    private String delayReason;

    @Schema(description = "备注", example = "加急")
    private String remark;

    @Schema(description = "是否延期（染色单交货日期 < 今天 且缸非 COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean delayed;

    @Schema(description = "未决问题数（反馈流水中标记为问题且未闭环的记录数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer unresolvedIssueCount;

    @Schema(description = "创建时间", example = "2026-09-28 10:00:00")
    private LocalDateTime createTime;

}

package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 生产管理-进度反馈排缸日历缸明细 Response VO")
@Data
public class ProgressFeedbackCalendarVatVO {

    @Schema(description = "反馈缸ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Long feedbackId;

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    private Long dyeingOrderId;

    @Schema(description = "染色颜色明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    private Long dyeingColorId;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "A-12")
    private String vatNo;

    @Schema(description = "排缸日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-09")
    private LocalDate scheduledDate;

    @Schema(description = "进度状态（SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DYEING")
    private String status;

    @Schema(description = "本缸数量", example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "单位（如 KG/M）", example = "KG")
    private String unit;

    @Schema(description = "客户色号", example = "C101")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "藏青")
    private String colorName;

    @Schema(description = "合同号", example = "CH2026-001")
    private String contractNo;

    @Schema(description = "承染方供应商ID", example = "5")
    private Long dyeingFactoryId;

    @Schema(description = "承染方公司名", example = "永盛染厂")
    private String dyeingFactoryName;

}

package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理-进度反馈排缸日历 Request VO")
@Data
public class ProgressFeedbackCalendarReqVO {

    @Schema(description = "月份（yyyy-MM，与 from/to 二选一）", example = "2026-10")
    private String month;

    @Schema(description = "起始日期（yyyy-MM-dd）", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate from;

    @Schema(description = "截止日期（yyyy-MM-dd）", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate to;

    @Schema(description = "承染方供应商ID", example = "5")
    private Long dyeingFactoryId;

    @Schema(description = "进度状态（SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", example = "DYEING")
    private String status;

}

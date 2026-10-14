package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理-排缸日历 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackCalendarReqVO {

    @Schema(description = "月份（格式 yyyy-MM，与 from/to 二选一）", example = "2026-10")
    private String month;

    @Schema(description = "起始日期（与 to 配对，与 month 二选一）", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate from;

    @Schema(description = "截止日期（与 from 配对，与 month 二选一）", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate to;

    @Schema(description = "承染方供应商ID（可选筛选）", example = "5")
    private Long dyeingFactoryId;

    @Schema(description = "缸状态（可选筛选，SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", example = "DYEING")
    private String status;

}

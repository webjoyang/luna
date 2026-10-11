package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理 - 甘特图排产查询 Request VO")
@Data
public class GanttQueryReqVO {

    @Schema(description = "窗口开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-01")
    @NotNull(message = "开始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate start;

    @Schema(description = "窗口结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-31")
    @NotNull(message = "结束日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate end;

    @Schema(description = "织机ID列表筛选（缺省全部 ACTIVE 织机）", example = "[1, 2]")
    private List<Long> loomIds;

    @Schema(description = "任务状态筛选（PENDING/IN_PROGRESS/HALTED/COMPLETED）", example = "[\"PENDING\", \"IN_PROGRESS\"]")
    private List<String> statuses;

    @AssertTrue(message = "结束日期不能早于开始日期")
    public boolean isDateRangeValid() {
        if (start == null || end == null) {
            return true;
        }
        return !end.isBefore(start);
    }

    @AssertTrue(message = "查询窗口跨度不能超过92天")
    public boolean isWindowSpanValid() {
        if (start == null || end == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(start, end) <= 92;
    }

}

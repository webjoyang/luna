package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理 - 排产甘特图 Request VO")
@Data
public class GanttReqVO {

    @Schema(description = "窗口开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-01")
    @NotNull(message = "窗口开始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate start;

    @Schema(description = "窗口结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-31")
    @NotNull(message = "窗口结束日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate end;

    @Schema(description = "织机ID筛选列表（缺省查全部 ACTIVE 织机）")
    private List<Long> loomIds;

    @Schema(description = "任务状态筛选列表（PENDING/IN_PROGRESS/HALTED/COMPLETED）")
    private List<String> statuses;

}

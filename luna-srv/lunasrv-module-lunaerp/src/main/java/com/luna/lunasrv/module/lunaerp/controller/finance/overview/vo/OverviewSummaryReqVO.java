package com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 财务管理 - 收支总览汇总 Request VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 收支总览汇总 Request VO")
@Data
public class OverviewSummaryReqVO {

    @Schema(description = "期间开始日期（缺省本月初）", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "期间结束日期（缺省本月末）", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

    @Schema(description = "资金账户ID（可选筛选）", example = "10")
    private Long accountId;

}

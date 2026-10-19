package com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理 - 收支总览综合汇总 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 收支总览综合汇总 Response VO")
@Data
public class OverviewSummaryRespVO {

    @Schema(description = "期间开始日期", example = "2026-10-01")
    private LocalDate periodStart;

    @Schema(description = "期间结束日期", example = "2026-10-31")
    private LocalDate periodEnd;

    @Schema(description = "资金账户ID筛选（若指定）", example = "10")
    private Long accountId;

    @Schema(description = "账户明细汇总列表")
    private List<AccountSummaryRespVO> accountList;

    @Schema(description = "合计行")
    private AccountSummaryRespVO total;

    @Schema(description = "按类别发生额汇总列表")
    private List<CategorySummaryRespVO> categoryList;

}

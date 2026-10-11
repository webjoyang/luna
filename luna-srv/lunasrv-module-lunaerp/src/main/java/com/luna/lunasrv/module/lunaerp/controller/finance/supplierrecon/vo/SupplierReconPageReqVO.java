package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商对账单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SupplierReconPageReqVO extends PageParam {

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "状态（DRAFT / CONFIRMED / CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "对账区间开始日期", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "对账区间结束日期", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

    @Schema(description = "关键词（对账单号或供应商名称）", example = "SR20261011001")
    private String keyword;

}

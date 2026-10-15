package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商对账预览 Request VO")
@Data
public class SupplierReconPreviewReqVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "对账区间起始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-01")
    @NotNull(message = "对账区间起始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-30")
    @NotNull(message = "对账区间截止日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

}

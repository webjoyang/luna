package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商对账单创建/更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconSaveReqVO {

    @Schema(description = "对账单ID（更新时必填）", example = "100")
    private Long id;

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "对账区间开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-01")
    @NotNull(message = "开始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "对账区间结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-30")
    @NotNull(message = "结束日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

    @Schema(description = "备注", example = "9月份结算")
    private String remark;

    @Schema(description = "明细列表")
    private List<SupplierReconItemSaveReqVO> items;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 客户对账单取数预览 Request VO")
@Data
public class CustomerStatementPrefetchReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "币种", requiredMode = Schema.RequiredMode.REQUIRED, example = "RMB")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "对账区间起始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-01")
    @NotNull(message = "对账起始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-30")
    @NotNull(message = "对账截止日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

}

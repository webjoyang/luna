package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 客户账龄汇总查询 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgingSummaryReqVO {

    @Schema(description = "账龄基准日（查询日，默认当前日期）", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate agingDate;

    @Schema(description = "客户ID列表（为空查全量客户）", example = "[1001, 1002]")
    private List<Long> customerIds;

    @Schema(description = "归属业务员ID（可选）", example = "2001")
    private Long salesmanId;

    @Schema(description = "币种（可选，默认 CNY）", example = "CNY")
    private String currency;

}

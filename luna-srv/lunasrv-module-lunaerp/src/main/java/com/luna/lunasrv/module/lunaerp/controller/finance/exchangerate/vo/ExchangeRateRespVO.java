package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务管理 - 汇率响应 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 汇率主档 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateRespVO {

    @Schema(description = "汇率ID", example = "1024")
    private Long id;

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "汇率", example = "7.23450000")
    private BigDecimal rate;

    @Schema(description = "生效日期", example = "2026-10-01")
    private LocalDate effectiveDate;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

    @Schema(description = "备注说明", example = "即期汇率")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

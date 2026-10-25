package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 汇率主档 Response VO")
@Data
public class ExchangeRateRespVO {

    @Schema(description = "汇率记录ID", example = "1024")
    private Long id;

    @Schema(description = "外币币种（USD/EUR/HKD等）", example = "USD")
    private String currency;

    @Schema(description = "汇率（1 外币 = rate 本位币）", example = "7.12345678")
    private BigDecimal rate;

    @Schema(description = "生效日期", example = "2026-10-16")
    private LocalDate effectiveDate;

    @Schema(description = "状态（ENABLE 启用 / DISABLE 停用）", example = "ENABLE")
    private String status;

    @Schema(description = "备注说明", example = "中国银行即期牌价")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

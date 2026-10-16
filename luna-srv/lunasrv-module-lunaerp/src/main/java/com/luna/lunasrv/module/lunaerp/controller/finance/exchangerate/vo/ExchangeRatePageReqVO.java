package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 汇率主档分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeRatePageReqVO extends PageParam {

    @Schema(description = "外币币种（USD/EUR/HKD等）", example = "USD")
    private String currency;

    @Schema(description = "状态（ENABLE 启用 / DISABLE 停用）", example = "ENABLE")
    private String status;

    @Schema(description = "生效日期", example = "2026-10-16")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

}

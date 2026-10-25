package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 汇率主档创建 Request VO")
@Data
public class ExchangeRateSaveReqVO {

    @Schema(description = "外币币种（USD/EUR/HKD 等，本位币 RMB/CNY 禁止配置）", requiredMode = Schema.RequiredMode.REQUIRED, example = "USD")
    @NotBlank(message = "币种不能为空")
    @Size(max = 8, message = "币种长度不能超过8位")
    private String currency;

    @Schema(description = "汇率（1 外币 = rate 本位币）", requiredMode = Schema.RequiredMode.REQUIRED, example = "7.12345678")
    @NotNull(message = "汇率不能为空")
    @DecimalMin(value = "0.00000001", message = "汇率必须大于0")
    @Digits(integer = 12, fraction = 8, message = "汇率整数位最多12位，小数位最多8位")
    private BigDecimal rate;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-16")
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "备注说明", example = "中国银行即期中行折算价")
    private String remark;

}

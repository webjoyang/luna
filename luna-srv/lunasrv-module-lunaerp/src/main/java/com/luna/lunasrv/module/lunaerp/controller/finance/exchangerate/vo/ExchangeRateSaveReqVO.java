package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 汇率保存请求 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 汇率主档保存 Request VO")
@Data
public class ExchangeRateSaveReqVO {

    @Schema(description = "汇率ID", example = "1024")
    private Long id;

    @Schema(description = "外币币种（如 USD/EUR/HKD 等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "USD")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "汇率（1 外币 = rate 本位币）", requiredMode = Schema.RequiredMode.REQUIRED, example = "7.23450000")
    @NotNull(message = "汇率不能为空")
    private BigDecimal rate;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-01")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

    @Schema(description = "备注说明", example = "中国银行即期汇率")
    private String remark;

}

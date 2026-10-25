package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "财务管理 - 客户账龄下钻明细 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAgingDetailReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @Schema(description = "账龄基准日（查询日），空时默认当前系统日期", example = "2026-10-18")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate agingDate;
}

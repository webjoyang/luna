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

@Schema(description = "财务管理 - 客户账龄汇总 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAgingSummaryReqVO {

    @Schema(description = "账龄基准日（查询日），空时默认当前系统日期", example = "2026-10-18")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate agingDate;

    @Schema(description = "客户ID列表筛选", example = "[100, 101]")
    private List<Long> customerIds;

    @Schema(description = "归属业务员用户ID筛选", example = "2048")
    private Long salespersonUserId;

    @Schema(description = "归属业务员用户ID筛选（salespersonUserId 别名）", example = "2048")
    private Long salesmanId;

    public Long getEffectiveSalespersonUserId() {
        return salespersonUserId != null ? salespersonUserId : salesmanId;
    }
}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 客户对账单生成 Request VO")
@Data
public class CustomerStatementCreateReqVO {

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

    @Schema(description = "自定义对账单号（为空则系统自动生成 CS{yyyy}{4位流水}）", example = "CS20260001")
    private String statementNo;

    @Schema(description = "选入分录ID集合（可选，为空则默认选入期间内全部未占用分录）")
    private List<Long> selectedEntryIds;

    @Schema(description = "手工追加分录ID集合（可选）")
    private List<Long> manualEntryIds;

    @Schema(description = "备注说明", example = "2026年9月份常规对账")
    private String remark;

}

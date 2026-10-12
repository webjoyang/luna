package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 客户对账单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomerStatementPageReqVO extends PageParam {

    @Schema(description = "对账单号（支持模糊）", example = "CS2026")
    private String statementNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称（支持模糊）", example = "织造厂")
    private String customerName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "状态（DRAFT/SENT/CONFIRMED/REJECTED/VOID）", example = "DRAFT")
    private String status;

    @Schema(description = "对账期间开始日期", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "对账期间结束日期", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

}

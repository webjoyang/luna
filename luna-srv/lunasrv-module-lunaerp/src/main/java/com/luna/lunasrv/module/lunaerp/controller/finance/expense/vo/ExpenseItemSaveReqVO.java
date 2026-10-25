package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 员工费用报销明细行保存 Request VO")
@Data
public class ExpenseItemSaveReqVO {

    @Schema(description = "明细行ID（修改行时传，新增留空）", example = "101")
    private Long id;

    @Schema(description = "费用类别（字典 2284 lunaerp_expense_category：TRAVEL/OFFICE/TRANSPORT/ENTERTAIN/RENT/UTILITY/COMM/OTHER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRAVEL")
    @NotBlank(message = "费用类别不能为空")
    private String category;

    @Schema(description = "原币金额（元，必须 > 0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "280.50")
    @NotNull(message = "报销金额不能为空")
    @DecimalMin(value = "0.01", message = "报销金额必须大于0")
    private BigDecimal originalAmount;

    @Schema(description = "费用发生日期（不晚于提交日）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-18")
    @NotNull(message = "费用发生日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate occurDate;

    @Schema(description = "费用明细备注说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "拜访客户打车发票")
    @NotBlank(message = "费用明细备注不能为空")
    private String remark;

}

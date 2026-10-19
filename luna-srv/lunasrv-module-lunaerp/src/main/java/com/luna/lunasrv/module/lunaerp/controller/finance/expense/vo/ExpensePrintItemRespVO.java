package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工费用报销单套打明细行 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePrintItemRespVO {

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "费用类别（字典 2284）", example = "差旅费")
    private String category;

    @Schema(description = "原币金额（元，套打豁免脱敏）", example = "280.50")
    private BigDecimal originalAmount;

    @Schema(description = "费用发生日期", example = "2026-10-18")
    private LocalDate occurDate;

    @Schema(description = "费用明细备注说明", example = "拜访客户打车发票")
    private String remark;

}

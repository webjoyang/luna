package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工工资表 A4 套打数据 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPrintRespVO {

    @Schema(description = "业务类型（固定 PAYROLL）", example = "PAYROLL")
    private String bizType;

    @Schema(description = "打印标题", example = "2026-10 工资表")
    private String title;

    @Schema(description = "工资单ID", example = "100")
    private Long id;

    @Schema(description = "工资单号", example = "GZ202610001")
    private String payrollNo;

    @Schema(description = "工资期间（YYYY-MM）", example = "2026-10")
    private String period;

    @Schema(description = "审批状态", example = "APPROVED")
    private String approvalStatus;

    @Schema(description = "应发总计（元，豁免脱敏）", example = "50000.00")
    private BigDecimal earningTotal;

    @Schema(description = "扣款总计（元，豁免脱敏）", example = "2000.00")
    private BigDecimal deductionTotal;

    @Schema(description = "实发总计（元，豁免脱敏）", example = "48000.00")
    private BigDecimal netAmount;

    @Schema(description = "提交审批时间")
    private LocalDateTime submitTime;

    @Schema(description = "审批通过时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注说明", example = "10月工资单")
    private String remark;

    @Schema(description = "打印生成时间")
    private LocalDateTime printTime;

    @Schema(description = "员工明细行列表（每人一行 应发/扣款/实发+签收栏）")
    private List<PayrollPrintEmployeeRespVO> items;

}

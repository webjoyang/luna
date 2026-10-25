package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 员工工资表 A4 套打员工行 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPrintEmployeeRespVO {

    @Schema(description = "序号", example = "1")
    private Integer lineNo;

    @Schema(description = "员工用户ID", example = "10")
    private Long userId;

    @Schema(description = "员工姓名快照", example = "张三")
    private String nickname;

    @Schema(description = "应发合计（元，豁免脱敏）", example = "5000.00")
    private BigDecimal earningTotal;

    @Schema(description = "扣款合计（元，豁免脱敏）", example = "200.00")
    private BigDecimal deductionTotal;

    @Schema(description = "实发合计（元，豁免脱敏）", example = "4800.00")
    private BigDecimal netAmount;

    @Schema(description = "签收栏", example = "张三")
    private String signature;

    @Schema(description = "备注", example = "全勤")
    private String remark;

}

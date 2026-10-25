package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 员工工资单作废 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCancelReqVO {

    @Schema(description = "工资单ID", example = "100")
    private Long id;

    @Schema(description = "作废原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "计件统计有误，经审批作废重新计算")
    @NotBlank(message = "作废原因不能为空")
    private String cancelReason;

}

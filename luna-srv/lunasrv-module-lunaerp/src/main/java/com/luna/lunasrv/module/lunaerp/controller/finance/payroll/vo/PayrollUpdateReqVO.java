package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 员工工资单更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollUpdateReqVO {

    @Schema(description = "工资单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工资单ID不能为空")
    private Long id;

    @Schema(description = "工资单标题", example = "2026-10 工资表（调整）")
    private String title;

    @Schema(description = "备注说明", example = "调整计件单价与扣款")
    private String remark;

    @Schema(description = "员工汇总行列表（整体替换保存，至少1行）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工资单员工明细至少需要一人")
    @Valid
    private List<PayrollEmployeeSaveReqVO> employees;

}

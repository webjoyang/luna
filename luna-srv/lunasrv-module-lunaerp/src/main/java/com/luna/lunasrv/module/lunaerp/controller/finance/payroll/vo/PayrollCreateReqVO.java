package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 员工工资单创建 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCreateReqVO {

    @Schema(description = "工资期间（YYYY-MM格式）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10")
    @NotBlank(message = "工资期间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "工资期间格式必须为 YYYY-MM")
    private String period;

    @Schema(description = "工资单标题（缺省 '{period} 工资表'）", example = "2026-10 工资表")
    private String title;

    @Schema(description = "备注说明", example = "2026年10月全厂工资表")
    private String remark;

    @Schema(description = "员工汇总行列表（可选，也可后续编辑保存）")
    @Valid
    private List<PayrollEmployeeSaveReqVO> employees;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 工资单员工汇总行保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEmployeeSaveReqVO {

    @Schema(description = "员工用户ID（system_users.id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "员工用户ID不能为空")
    private Long userId;

    @Schema(description = "员工备注说明", example = "全勤")
    private String remark;

    @Schema(description = "款项明细行列表")
    @Valid
    private List<PayrollLineItemSaveReqVO> items;

}

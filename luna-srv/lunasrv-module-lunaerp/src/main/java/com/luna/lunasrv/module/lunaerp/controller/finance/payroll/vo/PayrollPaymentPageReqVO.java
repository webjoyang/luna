package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 工资发放批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentPageReqVO extends PageParam {

    @Schema(description = "工资单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工资单ID不能为空")
    private Long payrollId;

}

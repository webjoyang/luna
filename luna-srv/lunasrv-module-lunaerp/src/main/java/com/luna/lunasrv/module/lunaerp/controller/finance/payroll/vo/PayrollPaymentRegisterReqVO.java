package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 工资发放登记 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentRegisterReqVO {

    @Schema(description = "工资单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工资单ID不能为空")
    private Long payrollId;

    @Schema(description = "付款资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "付款资金账户不能为空")
    private Long accountId;

    @Schema(description = "本次发放金额（元，缺省未发余额）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "发放支付日期（缺省当日）", example = "2026-10-19")
    private LocalDate payDate;

    @Schema(description = "备注说明", example = "银行代发首批")
    private String remark;

}

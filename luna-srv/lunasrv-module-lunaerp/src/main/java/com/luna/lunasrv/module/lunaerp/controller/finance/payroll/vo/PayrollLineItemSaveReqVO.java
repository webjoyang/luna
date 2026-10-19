package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 工资单款项明细行保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollLineItemSaveReqVO {

    @Schema(description = "款项大类（EARNING 收入 / DEDUCTION 扣款）", requiredMode = Schema.RequiredMode.REQUIRED, example = "EARNING")
    @NotBlank(message = "款项大类不能为空")
    private String itemKind;

    @Schema(description = "款项类型（EARNING: BASE/PIECE/COMMISSION/BONUS/SUBSIDY/OTHER; DEDUCTION: OTHER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "BASE")
    @NotBlank(message = "款项类型不能为空")
    private String itemType;

    @Schema(description = "计件数量（计件项适用）", example = "100.00")
    private BigDecimal qty;

    @Schema(description = "计件单价（元，计件项适用）", example = "5.50")
    private BigDecimal unitPrice;

    @Schema(description = "款项金额（元，必须 > 0，有数量和单价时服务端自动计算）", example = "550.00")
    private BigDecimal amount;

    @Schema(description = "款项说明", example = "10月基本工资")
    private String remark;

}

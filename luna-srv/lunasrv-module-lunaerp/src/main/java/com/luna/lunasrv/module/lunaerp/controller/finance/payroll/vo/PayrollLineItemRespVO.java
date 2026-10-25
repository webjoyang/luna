package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 工资单款项明细行 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollLineItemRespVO {

    @Schema(description = "款项明细行ID", example = "10")
    private Long id;

    @Schema(description = "工资单主表ID", example = "100")
    private Long payrollId;

    @Schema(description = "员工汇总行ID", example = "1001")
    private Long employeeRowId;

    @Schema(description = "款项大类（EARNING 收入 / DEDUCTION 扣款）", example = "EARNING")
    private String itemKind;

    @Schema(description = "款项类型（EARNING: BASE/PIECE/COMMISSION/BONUS/SUBSIDY/OTHER; DEDUCTION: DEFECT/OTHER）", example = "BASE")
    private String itemType;

    @Schema(description = "计件数量", example = "100.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal qty;

    @Schema(description = "计件单价（元）", example = "5.50")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unitPrice;

    @Schema(description = "款项金额（元）", example = "550.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "款项说明", example = "基本工资")
    private String remark;

    @Schema(description = "次品引用追溯明细列表（DEFECT类型适用）")
    private List<PayrollDefectRefRespVO> defectRefs;

}

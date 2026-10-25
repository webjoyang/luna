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

@Schema(description = "管理后台 - 工资单员工汇总行 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEmployeeRespVO {

    @Schema(description = "员工汇总行ID", example = "1001")
    private Long id;

    @Schema(description = "工资单主表ID", example = "100")
    private Long payrollId;

    @Schema(description = "员工用户ID", example = "10")
    private Long userId;

    @Schema(description = "员工昵称快照", example = "张三")
    private String nickname;

    @Schema(description = "员工应发合计（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal earningTotal;

    @Schema(description = "员工扣款合计（元）", example = "200.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal deductionTotal;

    @Schema(description = "员工实发合计（元）", example = "4800.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal netAmount;

    @Schema(description = "员工备注说明", example = "全勤")
    private String remark;

    @Schema(description = "款项明细行列表")
    private List<PayrollLineItemRespVO> items;

}

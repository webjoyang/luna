package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工费用报销明细行 Response VO")
@Data
public class ExpenseItemRespVO {

    @Schema(description = "明细行ID", example = "101")
    private Long id;

    @Schema(description = "报销单ID", example = "100")
    private Long expenseId;

    @Schema(description = "费用类别（字典 2284）", example = "TRAVEL")
    private String category;

    @Schema(description = "原币金额（元）", example = "280.50")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "费用发生日期", example = "2026-10-18")
    private LocalDate occurDate;

    @Schema(description = "费用明细备注说明", example = "拜访客户打车发票")
    private String remark;

}

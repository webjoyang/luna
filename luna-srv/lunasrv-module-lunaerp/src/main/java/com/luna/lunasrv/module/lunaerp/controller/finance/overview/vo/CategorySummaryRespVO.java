package com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务管理 - 分录类别发生额汇总 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 分录类别发生额汇总 Response VO")
@Data
public class CategorySummaryRespVO {

    @Schema(description = "分录类别代码（字典 lunaerp_finance_ledger_category）", example = "OUT_EXPENSE")
    private String category;

    @Schema(description = "主导收支方向（DEBIT 流入 / CREDIT 流出）", example = "CREDIT")
    private String direction;

    @Schema(description = "借方发生额（流入）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal debitAmount;

    @Schema(description = "贷方发生额（流出）", example = "20000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal creditAmount;

    @Schema(description = "本期总发生额（借方 + 贷方绝对值发生额）", example = "20000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "净发生额（借方 - 贷方）", example = "-20000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal netAmount;

    @Schema(description = "笔数", example = "5")
    private Long count;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 经营毛利参考（管理口径） Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 经营毛利参考（管理口径） Response VO")
@Data
public class GrossMarginRespVO {

    @Schema(description = "期间开始日期", example = "2026-10-01")
    private LocalDate periodStart;

    @Schema(description = "期间结束日期", example = "2026-10-31")
    private LocalDate periodEnd;

    @Schema(description = "本期收入（发货日口径 AR_INC - AR_INC_REVERSAL 净额）", example = "300000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal revenue;

    @Schema(description = "本期成本（确认日落区间 11号 confirmed 对账单 total_amount）", example = "200000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal cost;

    @Schema(description = "毛利差额（收入 - 成本）", example = "100000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal grossProfit;

    @Schema(description = "毛利率（毛利差额 / 收入，四位小数）", example = "0.3333")
    private BigDecimal grossMarginRate;

    @Schema(description = "应收侧是否有分录数据", example = "true")
    private Boolean hasArData;

    @Schema(description = "口径永久声明（管理参考、期间错配、非法定报表）", example = "管理参考、期间错配、非法定报表")
    private String disclaimer;

    @Schema(description = "空态说明（应收侧无数据时提示）", example = "应收侧暂无分录数据（待发货审核事件自动落账）")
    private String emptyStateNotice;

}

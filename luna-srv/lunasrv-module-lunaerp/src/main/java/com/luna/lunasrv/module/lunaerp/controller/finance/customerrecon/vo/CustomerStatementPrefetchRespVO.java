package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 客户对账单取数预览 Response VO")
@Data
public class CustomerStatementPrefetchRespVO {

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户公司名", example = "盛泽纺织实业有限公司")
    private String customerName;

    @Schema(description = "对账币种", example = "RMB")
    private String currency;

    @Schema(description = "对账区间起始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "期初欠款余额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal openingBalance;

    @Schema(description = "本期应收发生额（借方合计）", example = "12000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal periodDebit;

    @Schema(description = "本期收款核销额（贷方合计）", example = "8000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal periodCredit;

    @Schema(description = "期末应收余额（元，期初+借-贷）", example = "9000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal closingBalance;

    @Schema(description = "候选分录明细行列表")
    private List<CustomerStatementCandidateItemRespVO> lines;

}

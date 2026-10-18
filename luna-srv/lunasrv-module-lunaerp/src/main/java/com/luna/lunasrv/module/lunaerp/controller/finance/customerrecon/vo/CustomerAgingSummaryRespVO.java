package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "财务管理 - 客户账龄汇总一行 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAgingSummaryRespVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long customerId;

    @Schema(description = "客户公司名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "杭州明辉纺织有限公司")
    private String customerName;

    @Schema(description = "信用账期天数（空表示未设账期）", example = "30")
    private Integer creditDays;

    @Schema(description = "归属业务员用户ID", example = "2048")
    private Long salespersonUserId;

    @Schema(description = "归属业务员名称", example = "张经理")
    private String salespersonName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "合计应收余额（五桶之和）", requiredMode = Schema.RequiredMode.REQUIRED, example = "25000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalArBalance;

    @Schema(description = "0–30天应收", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal bucket0To30;

    @Schema(description = "31–60天应收", requiredMode = Schema.RequiredMode.REQUIRED, example = "8000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal bucket31To60;

    @Schema(description = "61–90天应收", requiredMode = Schema.RequiredMode.REQUIRED, example = "4000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal bucket61To90;

    @Schema(description = "91–180天应收", requiredMode = Schema.RequiredMode.REQUIRED, example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal bucket91To180;

    @Schema(description = "181+天应收", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal bucket181Plus;

    @Schema(description = "预收余额（单列展示不进桶）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal advanceBalance;

    @Schema(description = "其中已逾期金额（发货日+credit_days<查询日）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal overdueAmount;

    @Schema(description = "账龄基准日（查询日）", example = "2026-10-18")
    private LocalDate agingDate;
}

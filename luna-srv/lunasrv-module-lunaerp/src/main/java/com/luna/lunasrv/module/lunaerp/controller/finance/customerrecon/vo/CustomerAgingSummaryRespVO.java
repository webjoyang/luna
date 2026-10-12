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

@Schema(description = "管理后台 - 客户账龄汇总一行 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgingSummaryRespVO {

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称", example = "上海宏运纺织有限公司")
    private String customerName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "信用账期天数（空=未设账期）", example = "30")
    private Integer creditDays;

    @Schema(description = "业务员ID", example = "2001")
    private Long salesmanId;

    @Schema(description = "业务员姓名", example = "李销售")
    private String salesmanName;

    @Schema(description = "合计应收余额（未核销应收总额）", example = "10000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal totalBalance;

    @Schema(description = "账龄 0-30 天金额", example = "3000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal bucket0To30;

    @Schema(description = "账龄 31-60 天金额", example = "2000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal bucket31To60;

    @Schema(description = "账龄 61-90 天金额", example = "1500.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal bucket61To90;

    @Schema(description = "账龄 91-180 天金额", example = "2500.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal bucket91To180;

    @Schema(description = "账龄 181 天以上金额", example = "1000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal bucket181Plus;

    @Schema(description = "其中已逾期金额", example = "3500.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal overdueAmount;

    @Schema(description = "预收余额（单列不进桶）", example = "5000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal advanceBalance;

}

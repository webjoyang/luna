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

@Schema(description = "管理后台 - 客户账龄未核销明细分录 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgingDetailRespVO {

    @Schema(description = "经营分录ID", example = "10001")
    private Long ledgerEntryId;

    @Schema(description = "发货日/业务日期", example = "2026-09-15")
    private LocalDate bizDate;

    @Schema(description = "业务源单类型", example = "SHIPMENT")
    private String refType;

    @Schema(description = "业务源单ID", example = "2001")
    private Long refId;

    @Schema(description = "业务源单单号", example = "SH20260915001")
    private String refNo;

    @Schema(description = "原始应收发生额", example = "5000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "已核销金额", example = "2000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal settledAmount;

    @Schema(description = "未核销余额（应收余额）", example = "3000.00")
    @Sensitive(type = SensitiveType.AMOUNT, seePermission = SensitiveConstants.PERMISSION_AMOUNT)
    private BigDecimal unsettledAmount;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "账龄天数（查询日 - 发货日）", example = "46")
    private Long agingDays;

    @Schema(description = "所属账龄区间（0-30/31-60/61-90/91-180/181+）", example = "31-60")
    private String bucket;

    @Schema(description = "信用账期天数", example = "30")
    private Integer creditDays;

    @Schema(description = "到期日（发货日 + credit_days）", example = "2026-10-15")
    private LocalDate dueDate;

    @Schema(description = "是否已逾期", example = "true")
    private Boolean isOverdue;

    @Schema(description = "逾期天数（若逾期则为 查询日 - 到期日，否则为 0）", example = "16")
    private Long overdueDays;

    @Schema(description = "摘要/备注", example = "发货单出库应收")
    private String summary;

}

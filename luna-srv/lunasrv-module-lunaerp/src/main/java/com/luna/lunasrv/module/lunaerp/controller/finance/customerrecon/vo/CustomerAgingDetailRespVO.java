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

@Schema(description = "财务管理 - 客户未核销明细分录下钻 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAgingDetailRespVO {

    @Schema(description = "分录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long ledgerEntryId;

    @Schema(description = "发货日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-15")
    private LocalDate bizDate;

    @Schema(description = "业务关联类型（SHIPMENT_ITEM / SHIPMENT_FREIGHT）", example = "SHIPMENT_ITEM")
    private String refType;

    @Schema(description = "业务关联ID（发货单ID）", example = "5001")
    private Long refId;

    @Schema(description = "业务关联明细ID（发货明细ID）", example = "6001")
    private Long refItemId;

    @Schema(description = "发货单号", example = "FH202608150001")
    private String refNo;

    @Schema(description = "关联合同号", example = "HT20260701001")
    private String contractNo;

    @Schema(description = "品名/摘要", example = "精梳全棉纱 32支")
    private String productName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "原始发生金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "已核销/冲减金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal writtenOffAmount;

    @Schema(description = "未核销余额（账龄计算基准额）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal remainingAmount;

    @Schema(description = "账龄天数（查询日 - 发货日）", requiredMode = Schema.RequiredMode.REQUIRED, example = "45")
    private Integer agingDays;

    @Schema(description = "所属五桶区间（0-30 / 31-60 / 61-90 / 91-180 / 181+）", requiredMode = Schema.RequiredMode.REQUIRED, example = "31-60")
    private String bucket;

    @Schema(description = "客户信用账期天数", example = "30")
    private Integer creditDays;

    @Schema(description = "应到期日（发货日 + credit_days）", example = "2026-09-14")
    private LocalDate dueDate;

    @Schema(description = "是否已逾期（发货日 + credit_days < 查询日）", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean isOverdue;

    @Schema(description = "逾期天数（已逾期时为查询日 - 应到期日，未逾期为0）", example = "15")
    private Integer overdueDays;
}

package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

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

@Schema(description = "管理后台 - 发票候选发货单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCandidateShipmentRespVO {

    @Schema(description = "发货单ID", example = "200")
    private Long id;

    @Schema(description = "发货单号", example = "SH-20261012-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "发货日期", example = "2026-10-12")
    private LocalDate shippingDate;

    @Schema(description = "发货单货物金额（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "发货单运费（元）", example = "500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal freightFee;

    @Schema(description = "发货单可开票总额（货物金额+运费）", example = "10500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal shipmentAmount;

    @Schema(description = "累计已占用开票金额（SUBMITTED+ISSUED）", example = "4000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal invoicedAmount;

    @Schema(description = "本次可开票余额（默认本次开票金额）", example = "6500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal uninvoicedBalance;

}

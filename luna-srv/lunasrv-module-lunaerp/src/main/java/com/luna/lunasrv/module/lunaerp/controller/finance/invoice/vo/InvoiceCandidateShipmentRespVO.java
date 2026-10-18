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

@Schema(description = "管理后台 - 发票申请候选发货单 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCandidateShipmentRespVO {

    @Schema(description = "发货单ID", example = "2001")
    private Long shipmentId;

    @Schema(description = "发货单号", example = "SH-20261018-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户公司名称", example = "盛泽纺织")
    private String customerName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "发货日期", example = "2026-10-18")
    private LocalDate shippingDate;

    @Schema(description = "销售合同ID", example = "3001")
    private Long salesContractId;

    @Schema(description = "销售合同号", example = "SC-20261018-001")
    private String contractNo;

    @Schema(description = "货物金额（元）", example = "9000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "运费（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal freightFee;

    @Schema(description = "发货单价税合计总额（元，货物金额+运费）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal shipmentAmount;

    @Schema(description = "已占用的累计开票额（元，已开票与在途申请）", example = "3000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal invoicedAmount;

    @Schema(description = "可开票余额（元，发货总额 - 累计开票额）", example = "7000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal availableAmount;

}

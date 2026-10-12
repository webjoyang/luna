package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

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

@Schema(description = "管理后台 - 客户未核销应收分录 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArBalanceRespVO {

    @Schema(description = "分录ID", example = "1001")
    private Long ledgerEntryId;

    @Schema(description = "单号快照（发货单号/分录单号）", example = "SH20261012001")
    private String entryNo;

    @Schema(description = "分录类别（AR_INC 等）", example = "AR_INC")
    private String category;

    @Schema(description = "业务日期", example = "2026-10-12")
    private LocalDate bizDate;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "分录原始发生金额（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "已红冲金额（元）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal reversedAmount;

    @Schema(description = "已核销金额（元）", example = "4000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocatedAmount;

    @Schema(description = "当前未核销余额（元）", example = "6000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unallocatedBalance;

    @Schema(description = "源单据类型（SHIPMENT 等）", example = "SHIPMENT")
    private String refType;

    @Schema(description = "源单据ID", example = "200")
    private Long refId;

    @Schema(description = "发货单号（若关联发货单）", example = "SH-20261012-001")
    private String shipmentNo;

    @Schema(description = "销售合同号（若有关联合同）", example = "HT20261001")
    private String contractNo;

}

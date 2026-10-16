package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户未核销明细分录 Response VO（三视角选单数据源）")
@Data
public class ArBalanceRespVO {

    @Schema(description = "分录ID（AR_INC）", example = "1001")
    private Long entryId;

    @Schema(description = "发货业务日期", example = "2026-10-10")
    private LocalDate bizDate;

    @Schema(description = "来源业务类型（SHIPMENT_ITEM / SHIPMENT_FREIGHT）", example = "SHIPMENT_ITEM")
    private String refType;

    @Schema(description = "发货单ID", example = "2001")
    private Long refId;

    @Schema(description = "发货明细ID", example = "3001")
    private Long refItemId;

    @Schema(description = "发货单号", example = "SH-20261010-001")
    private String refNo;

    @Schema(description = "合同ID", example = "4001")
    private Long contractId;

    @Schema(description = "合同号", example = "HT-20261001-001")
    private String contractNo;

    @Schema(description = "对账单ID", example = "5001")
    private Long statementId;

    @Schema(description = "对账单号（若已纳入 CONFIRMED 对账单）", example = "CS20260001")
    private String statementNo;

    @Schema(description = "品名/摘要说明", example = "全棉弹力府绸")
    private String productName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "分录原发生金额（元）", example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "已核销金额（元）", example = "500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocatedAmount;

    @Schema(description = "未核销余额（元）", example = "1500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unallocatedAmount;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户对账单候选分录明细 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementCandidateItemVO {

    @Schema(description = "经营分录ID", example = "501")
    private Long ledgerEntryId;

    @Schema(description = "业务日期", example = "2026-09-15")
    private LocalDate bizDate;

    @Schema(description = "借贷方向（DEBIT/CREDIT）", example = "DEBIT")
    private String direction;

    @Schema(description = "分录类别（AR_INC/RECEIPT_WRITE_OFF等）", example = "AR_INC")
    private String category;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "源单类型（SHIPMENT/PAYMENT等）", example = "SHIPMENT")
    private String refType;

    @Schema(description = "源单ID", example = "1001")
    private Long refId;

    @Schema(description = "源单单号快照", example = "SH20260915001")
    private String refNo;

    @Schema(description = "摘要/品名或事由", example = "发货出库 - 纯棉色织布")
    private String summary;

    @Schema(description = "原始金额/净额", example = "2500.00")
    private BigDecimal amount;

    @Schema(description = "拟借方发生额", example = "2500.00")
    private BigDecimal debitAmount;

    @Schema(description = "拟贷方发生额", example = "0.00")
    private BigDecimal creditAmount;

    @Schema(description = "签收状态标记（仅展示标记，不阻塞对账）", example = "SIGNED")
    private String signStatus;

    @Schema(description = "是否已被非VOID对账单占用", example = "false")
    private Boolean occupied;

    @Schema(description = "占用对账单号（若已占用）", example = "CS20260001")
    private String occupiedStatementNo;

    @Schema(description = "占用对账单状态（若已占用，CONFIRMED 为强占用，DRAFT/SENT 为弱占用）", example = "CONFIRMED")
    private String occupiedStatus;

}

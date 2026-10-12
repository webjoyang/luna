package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 自动核销分配行 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateItemRespVO {

    @Schema(description = "关联应收分录ID", example = "1001")
    private Long ledgerEntryId;

    @Schema(description = "单号快照（发货单号/分录单号）", example = "SH20261012001")
    private String entryNo;

    @Schema(description = "业务日期", example = "2026-10-12")
    private LocalDate bizDate;

    @Schema(description = "分录原始金额（元）", example = "10000.00")
    private BigDecimal originalAmount;

    @Schema(description = "当前未核销余额（元）", example = "6000.00")
    private BigDecimal unallocatedBalance;

    @Schema(description = "本次建议分配金额（元）", example = "6000.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "源单类型（SHIPMENT等）", example = "SHIPMENT")
    private String refType;

    @Schema(description = "源单ID", example = "200")
    private Long refId;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户收款单核销明细保存 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptRefSaveVO {

    @Schema(description = "明细ID（修改草稿时传）", example = "10")
    private Long id;

    @Schema(description = "关联核销应收分录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "核销分录ID不能为空")
    private Long ledgerEntryId;

    @Schema(description = "本次核销分配金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "分配金额不能为空")
    @DecimalMin(value = "0.01", message = "分配金额必须大于0")
    private BigDecimal allocateAmount;

    @Schema(description = "分录单号快照", example = "SH20261012001")
    private String entryNo;

    @Schema(description = "业务日期快照", example = "2026-10-12")
    private LocalDate bizDate;

}

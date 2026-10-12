package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 发票申请明细行保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemSaveVO {

    @Schema(description = "明细ID（更新时传）", example = "10")
    private Long id;

    @Schema(description = "关联发货单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "关联发货单不能为空")
    private Long shipmentId;

    @Schema(description = "本次申请开票金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "本次开票金额不能为空")
    @DecimalMin(value = "0.01", message = "本次开票金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "行备注", example = "首批货物开票")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "成本卡试算 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardTrialRespVO {

    @Schema(description = "计费基准口径（KG / METER）", example = "KG")
    private String basis;

    @Schema(description = "每米重量 KG/M", example = "0.360000")
    private BigDecimal kgPerMeter;

    @Schema(description = "纱线成本 元/KG", example = "35.5000")
    private BigDecimal yarnCostPerKg;

    @Schema(description = "纱线成本 元/M", example = "10.6500")
    private BigDecimal yarnCostPerMeter;

    @Schema(description = "费用合计 元/KG", example = "8.0000")
    private BigDecimal totalFeePerKg;

    @Schema(description = "费用合计 元/M", example = "2.4000")
    private BigDecimal totalFeePerMeter;

    @Schema(description = "不含税单位成本 元/KG", example = "43.5000")
    private BigDecimal unitCostPerKg;

    @Schema(description = "含税单位成本 元/KG", example = "49.1550")
    private BigDecimal unitCostPerKgWithTax;

    @Schema(description = "不含税单位成本 元/M", example = "13.0500")
    private BigDecimal unitCostPerMeter;

    @Schema(description = "含税单位成本 元/M", example = "14.7465")
    private BigDecimal unitCostPerMeterWithTax;

    @Schema(description = "缺价纱线行数", example = "0")
    private Integer missingPriceCount;

    @Schema(description = "纱线明细行试算结果列表")
    private List<CostCardYarnItemVO> yarnItems;

    @Schema(description = "费用明细行试算结果列表")
    private List<CostCardFeeItemVO> feeItems;

}

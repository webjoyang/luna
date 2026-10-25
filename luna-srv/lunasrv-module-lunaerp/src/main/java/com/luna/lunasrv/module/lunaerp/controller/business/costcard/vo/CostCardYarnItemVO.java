package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "成本卡纱线明细行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardYarnItemVO {

    @Schema(description = "明细行ID", example = "100")
    private Long id;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "纱线规格不能为空")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S 精梳棉")
    private String yarnSpecName;

    @Schema(description = "经纬方向（GENERAL / WARP / WEFT）", example = "GENERAL")
    private String direction;

    @Schema(description = "配比百分比（49 表示 49%）", requiredMode = Schema.RequiredMode.REQUIRED, example = "49.00")
    @NotNull(message = "配比不能为空")
    private BigDecimal ratio;

    @Schema(description = "密度", example = "100.00")
    private BigDecimal density;

    @Schema(description = "织缩率%", example = "6.50")
    private BigDecimal crimpRate;

    @Schema(description = "投入损耗率%", example = "5.00")
    private BigDecimal wasteRate;

    @Schema(description = "每米用量 KG/M", example = "0.085000")
    private BigDecimal consumptionKgPerMeter;

    @Schema(description = "单价快照 元/KG", example = "35.5000")
    private BigDecimal price;

    @Schema(description = "取价价类", example = "PURCHASE")
    private String priceType;

    @Schema(description = "价格来源（PRICE_TABLE / SPEC_REF_FALLBACK / MANUAL）", example = "PRICE_TABLE")
    private String priceSource;

    @Schema(description = "取价生效日期", example = "2026-10-10")
    private LocalDate priceEffectiveDate;

    @Schema(description = "行成本贡献 元/KG", example = "18.2500")
    private BigDecimal contributionPerKg;

    @Schema(description = "行成本贡献 元/M", example = "5.4750")
    private BigDecimal contributionPerMeter;

}

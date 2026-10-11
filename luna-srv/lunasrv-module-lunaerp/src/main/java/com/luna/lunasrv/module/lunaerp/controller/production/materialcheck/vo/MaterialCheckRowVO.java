package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产管理 - 齐套检查纱线规格聚合行 VO（ADR-0041）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理 - 齐套检查纱线规格聚合行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCheckRowVO {

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 普梳棉")
    private String yarnSpecName;

    @Schema(description = "总需求量 (KG, 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "600.00")
    private BigDecimal demandQty;

    @Schema(description = "可用库存 (KG, 实时台账余额, 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "350.00")
    private BigDecimal stockQty;

    @Schema(description = "在途量 (KG, ACTIVE 采购单合计, 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    private BigDecimal inTransitQty;

    @Schema(description = "缺口（不含在途） (KG, max(0, 需求 - 可用), 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "250.00")
    private BigDecimal shortageWithoutInTransit;

    @Schema(description = "缺口（含在途） (KG, max(0, 需求 - 可用 - 在途), 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal shortageWithInTransit;

    @Schema(description = "建议采购量 (KG, = 缺口（含在途）, 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal suggestedPurchaseQty;

    @Schema(description = "各织造单贡献展开列表")
    private List<MaterialCheckContributionVO> contributions;

}

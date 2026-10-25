package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 齐套检查纱线规格聚合行 VO")
@Data
public class MaterialCheckRowVO {

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "总需求量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal requiredQty;

    @Schema(description = "可用库存量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "600.00")
    private BigDecimal availableQty;

    @Schema(description = "在途采购量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "200.00")
    private BigDecimal inTransitQty;

    @Schema(description = "缺口（不含在途）（KG，max(0, 需求 - 可用)）", requiredMode = Schema.RequiredMode.REQUIRED, example = "400.00")
    private BigDecimal shortageExcludeInTransit;

    @Schema(description = "缺口（含在途）（KG，max(0, 需求 - 可用 - 在途)）", requiredMode = Schema.RequiredMode.REQUIRED, example = "200.00")
    private BigDecimal shortageIncludeInTransit;

    @Schema(description = "建议采购量（KG，= 缺口（含在途））", requiredMode = Schema.RequiredMode.REQUIRED, example = "200.00")
    private BigDecimal suggestedPurchaseQty;

    @Schema(description = "各织造单贡献展开明细")
    private List<MaterialCheckContributionVO> contributions;

}

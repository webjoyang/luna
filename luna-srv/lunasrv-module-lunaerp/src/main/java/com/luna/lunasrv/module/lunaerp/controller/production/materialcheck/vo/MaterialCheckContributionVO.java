package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理 - 齐套检查织造单贡献展开行 VO")
@Data
public class MaterialCheckContributionVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long weavingOrderId;

    @Schema(description = "织造单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ20261001-01")
    private String weavingOrderCode;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long productId;

    @Schema(description = "产品编码", example = "PROD-001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉汗布")
    private String productName;

    @Schema(description = "织造单计划匹数", example = "50")
    private Integer plannedQty;

    @Schema(description = "织造单计划重量（KG）", example = "1000.00")
    private BigDecimal plannedWeight;

    @Schema(description = "BOM配比百分比（如 49.00 表示 49%）", example = "49.00")
    private BigDecimal ratio;

    @Schema(description = "本单需求量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "490.00")
    private BigDecimal requiredQty;

}

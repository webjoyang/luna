package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 生产管理 - 齐套检查织造单贡献展开行 VO（ADR-0041）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理 - 齐套检查织造单贡献展开行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCheckContributionVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long weavingOrderId;

    @Schema(description = "织造单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ-PB20261001-01")
    private String weavingOrderCode;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉汗布")
    private String productName;

    @Schema(description = "计划重量 (KG)", example = "500.00")
    private BigDecimal plannedWeight;

    @Schema(description = "配比百分比数字（如 49 表示 49%）", example = "49.00")
    private BigDecimal ratio;

    @Schema(description = "本单需求量 (KG, 2位小数)", requiredMode = Schema.RequiredMode.REQUIRED, example = "245.00")
    private BigDecimal demandQty;

}

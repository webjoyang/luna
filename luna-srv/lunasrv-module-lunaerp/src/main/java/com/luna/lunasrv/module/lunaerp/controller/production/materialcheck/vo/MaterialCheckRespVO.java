package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产管理 - 齐套检查试算 Response VO（ADR-0041）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理 - 齐套检查试算 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCheckRespVO {

    @Schema(description = "各纱线规格齐套聚合行")
    private List<MaterialCheckRowVO> rows;

    @Schema(description = "缺少配比BOM的织造单列表（不参与行聚合）")
    private List<MissingOrderVO> missingBomOrders;

    @Schema(description = "缺少计划重量/匹数的织造单列表（不参与行聚合）")
    private List<MissingOrderVO> missingWeightOrders;

    @Schema(description = "缺料规格数（随 includeInTransit 口径切换）", example = "2")
    private Integer shortageSpecCount;

    @Schema(description = "缺料合计 KG（随 includeInTransit 口径切换）", example = "500.00")
    private BigDecimal totalShortageQty;

    @Schema(description = "无法试算织造单信息 VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingOrderVO {

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

        @Schema(description = "计划匹数", example = "10")
        private Integer plannedQty;

        @Schema(description = "计划重量 (KG)", example = "0.00")
        private BigDecimal plannedWeight;

        @Schema(description = "原因说明", example = "未配置纱线配比BOM")
        private String reason;

    }

}

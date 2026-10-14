package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 齐套检查试算 Response VO")
@Data
public class MaterialCheckRespVO {

    @Schema(description = "纱线规格聚合行列表")
    private List<MaterialCheckRowVO> rows = Collections.emptyList();

    @Schema(description = "缺少配比BOM的织造单列表（不参与聚合行计算）")
    private List<MaterialCheckMissingOrderVO> missingBomOrders = Collections.emptyList();

    @Schema(description = "缺少计划重量或计划匹数的织造单列表（不参与聚合行计算）")
    private List<MaterialCheckMissingOrderVO> missingWeightOrders = Collections.emptyList();

    @Schema(description = "缺料规格数（根据 includeInTransit 口径统计）", example = "3")
    private Integer shortageSpecCount = 0;

    @Schema(description = "缺料合计数量（KG，根据 includeInTransit 口径统计）", example = "1250.00")
    private BigDecimal totalShortageQty = BigDecimal.ZERO;

}

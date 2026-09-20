package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-纱线采购测算胚布单展示快照 Response VO")
@Data
public class YarnOrderCalculateGreigeOrderRespVO {

    @Schema(description = "胚布单ID", example = "101")
    private Long id;

    @Schema(description = "胚布单号", example = "PB0920001")
    private String orderNo;

    @Schema(description = "产品ID", example = "201")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    private String productName;

    @Schema(description = "成品量", example = "1000.00")
    private BigDecimal finishedQty;

    @Schema(description = "预设损耗", example = "0.05")
    private BigDecimal suggestedLoss;

}

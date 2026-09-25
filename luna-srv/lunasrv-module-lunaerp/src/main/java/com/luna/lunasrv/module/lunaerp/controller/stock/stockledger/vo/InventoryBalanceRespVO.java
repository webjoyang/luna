package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 库存管理 - 库存余额 Response VO")
@Data
public class InventoryBalanceRespVO {

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long productId;

    @Schema(description = "产品编码", example = "PROD001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉胚布")
    private String productName;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "原料1号仓")
    private String warehouseName;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    private String unit;

    @Schema(description = "库存数量（实时聚合）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.50")
    private BigDecimal quantity;

}

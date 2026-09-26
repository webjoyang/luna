package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 库存管理 - 库存余额 Response VO")
@Data
public class InventoryBalanceRespVO {

    @Schema(description = "物品类型（PRODUCT / YARN_SPEC）", example = "PRODUCT")
    private String itemType;

    @Schema(description = "产品ID（物品为 PRODUCT 时有值）", example = "10")
    private Long productId;

    @Schema(description = "产品编码", example = "PROD001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉胚布")
    private String productName;

    @Schema(description = "纱线规格ID（物品为 YARN_SPEC 时有值）", example = "101")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "原料1号仓")
    private String warehouseName;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    private String unit;

    @Schema(description = "库存数量（实时聚合）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.50")
    private BigDecimal quantity;

}

package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 库存管理 - 库位分配放置记录保存 Request VO")
@Data
public class StockInItemPlacementSaveReqVO {

    @Schema(description = "放置记录ID（更新时可传）", example = "1024")
    private Long id;

    @Schema(description = "所属入库明细ID（更新时可传，防御性校验）", example = "200")
    private Long stockInItemId;

    @Schema(description = "库位ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "库位不能为空")
    private Long locationId;

    @Schema(description = "放置匹数", example = "5")
    private Integer pieceCount;

    @Schema(description = "放置数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @NotNull(message = "放置数量不能为空")
    @DecimalMin(value = "0.01", message = "放置数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "单位（跟随明细，可不传）", example = "KG")
    private String unit;

    @Schema(description = "备注", example = "放于A区货架首层")
    private String remark;

}

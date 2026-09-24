package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 库存管理 - 库位分配放置记录 Response VO")
@Data
public class StockInItemPlacementRespVO {

    @Schema(description = "放置记录ID", example = "1024")
    private Long id;

    @Schema(description = "所属入库单ID", example = "100")
    private Long stockInId;

    @Schema(description = "所属入库明细ID", example = "200")
    private Long stockInItemId;

    @Schema(description = "库位ID", example = "300")
    private Long locationId;

    @Schema(description = "库位编码快照", example = "A-01-01")
    private String locationCode;

    @Schema(description = "放置匹数", example = "5")
    private Integer pieceCount;

    @Schema(description = "放置数量", example = "50.00")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "备注", example = "放于A区货架首层")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

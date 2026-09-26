package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 库存管理 - 入库明细 Response VO")
@Data
public class StockInItemRespVO {

    @Schema(description = "明细ID", example = "1024")
    private Long id;

    @Schema(description = "所属入库单ID", example = "100")
    private Long stockInId;

    @Schema(description = "物品类型（PRODUCT / YARN_SPEC）", example = "PRODUCT")
    private String itemType;

    @Schema(description = "关联产品ID", example = "2048")
    private Long productId;

    @Schema(description = "产品编码快照", example = "P-1001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "关联纱线规格ID", example = "101")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称快照", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "批次号/缸号", example = "BATCH-20260924")
    private String batchNo;

    @Schema(description = "卷号", example = "R001")
    private String rollNo;

    @Schema(description = "匹数", example = "1")
    private Integer pieceCount;

    @Schema(description = "数量", example = "100.50")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "单价", example = "25.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "2512.50")
    private BigDecimal amount;

    @Schema(description = "备注", example = "合格品")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "库位分配列表")
    private List<StockInItemPlacementRespVO> placements;

}

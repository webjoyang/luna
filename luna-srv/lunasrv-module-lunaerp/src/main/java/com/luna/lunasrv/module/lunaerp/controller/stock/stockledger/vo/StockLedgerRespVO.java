package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 库存管理 - 库存流水台账 Response VO")
@Data
public class StockLedgerRespVO {

    @Schema(description = "流水ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "关联产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PROD001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉胚布")
    private String productName;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "仓库名称快照", example = "原料1号仓")
    private String warehouseName;

    @Schema(description = "库位ID", example = "5")
    private Long locationId;

    @Schema(description = "库位编码快照", example = "A-01-01")
    private String locationCode;

    @Schema(description = "批次号/缸号", example = "BATCH20260901")
    private String batchNo;

    @Schema(description = "交易类型（STOCK_IN 入库 / STOCK_IN_REVERSAL 红冲）", requiredMode = Schema.RequiredMode.REQUIRED, example = "STOCK_IN")
    private String txnType;

    @Schema(description = "带符号数量（入库为正，红冲为负）", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    private String unit;

    @Schema(description = "单价快照", example = "12.50")
    private BigDecimal unitPrice;

    @Schema(description = "金额快照（带符号，红冲为负）", example = "6250.00")
    private BigDecimal amount;

    @Schema(description = "关联单据类型（本期固定 STOCK_IN）", requiredMode = Schema.RequiredMode.REQUIRED, example = "STOCK_IN")
    private String relatedDocType;

    @Schema(description = "关联入库单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long relatedDocId;

    @Schema(description = "关联入库单号快照", example = "IN-20260925-001")
    private String relatedDocNo;

    @Schema(description = "备注", example = "正常入库")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "流水产生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

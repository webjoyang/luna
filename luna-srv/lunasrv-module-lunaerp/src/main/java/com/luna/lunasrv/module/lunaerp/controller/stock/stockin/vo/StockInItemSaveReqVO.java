package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 库存管理 - 入库明细保存 Request VO")
@Data
public class StockInItemSaveReqVO {

    @Schema(description = "明细ID（更新时传，可空）", example = "1024")
    private Long id;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "产品不能为空")
    private Long productId;

    @Schema(description = "批次号/缸号", example = "BATCH-20260924")
    private String batchNo;

    @Schema(description = "卷号", example = "R001")
    private String rollNo;

    @Schema(description = "匹数（缺省 1）", example = "1")
    private Integer pieceCount;

    @Schema(description = "数量（必填，>0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "单位（字典 lunaerp_stock_in_unit：KG/M）", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    @NotBlank(message = "单位不能为空")
    private String unit;

    @Schema(description = "单价", example = "25.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额（quantity * unitPrice，服务端权威重算）", example = "2512.50")
    private BigDecimal amount;

    @Schema(description = "备注", example = "合格品")
    private String remark;

}

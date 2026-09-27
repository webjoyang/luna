package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 发货明细创建/修改 Request VO")
@Data
public class ShipmentItemSaveReqVO {

    @Schema(description = "明细ID", example = "100")
    private Long id;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "产品不能为空")
    private Long productId;

    @Schema(description = "客户色号", example = "WH-01")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "本白")
    private String colorName;

    @Schema(description = "缸号/批次号", example = "LOT-202609-01")
    private String vatNo;

    @Schema(description = "发货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "发货数量不能为空")
    @DecimalMin(value = "0.01", message = "发货数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "2")
    private Integer pieces;

    @Schema(description = "单位（字典 lunaerp_stock_in_unit：KG/M）", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    @NotBlank(message = "单位不能为空")
    private String unit;

    @Schema(description = "单价", example = "25.00")
    private BigDecimal unitPrice;

    @Schema(description = "来源入库明细ID", example = "300")
    private Long sourceStockInItemId;

    @Schema(description = "明细备注", example = "送质检留样")
    private String remark;

    @Schema(description = "卷清单列表")
    @Valid
    private List<ShipmentRollSaveReqVO> rolls;

}

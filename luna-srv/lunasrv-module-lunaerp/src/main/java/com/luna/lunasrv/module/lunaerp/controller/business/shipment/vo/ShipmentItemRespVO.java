package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 发货明细 Response VO")
@Data
public class ShipmentItemRespVO {

    @Schema(description = "明细ID", example = "100")
    private Long id;

    @Schema(description = "所属发货单ID", example = "10")
    private Long shipmentId;

    @Schema(description = "产品ID", example = "200")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PRD-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉斜纹")
    private String productName;

    @Schema(description = "客户色号", example = "WH-01")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "本白")
    private String colorName;

    @Schema(description = "缸号/批次号", example = "LOT-202609-01")
    private String vatNo;

    @Schema(description = "发货数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "2")
    private Integer pieces;

    @Schema(description = "单位（字典 lunaerp_stock_in_unit：KG/M）", example = "KG")
    private String unit;

    @Schema(description = "单价", example = "25.00")
    private BigDecimal unitPrice;

    @Schema(description = "行金额（服务端重算：数量×单价）", example = "2500.00")
    private BigDecimal amount;

    @Schema(description = "来源入库明细ID", example = "300")
    private Long sourceStockInItemId;

    @Schema(description = "明细备注", example = "送质检留样")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "卷清单列表")
    private List<ShipmentRollRespVO> rolls;

}

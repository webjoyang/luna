package com.luna.lunasrv.module.lunaerp.controller.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "客户门户 - 发货单逐行明细 Response VO")
@Data
public class PortalShipmentItemRespVO {

    @Schema(description = "明细ID", example = "101")
    private Long id;

    @Schema(description = "所属发货单ID", example = "1001")
    private Long shipmentId;

    @Schema(description = "产品ID", example = "201")
    private Long productId;

    @Schema(description = "产品编码/规格", example = "P-2026-001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉斜纹布")
    private String productName;

    @Schema(description = "客户色号", example = "C-88")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "藏青")
    private String colorName;

    @Schema(description = "缸号/批次号", example = "GH-20260901")
    private String vatNo;

    @Schema(description = "发货数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "卷数/匹数", example = "5")
    private Integer pieces;

    @Schema(description = "单位", example = "M")
    private String unit;

    @Schema(description = "单价（金额透明，不脱敏）", example = "25.50")
    private BigDecimal unitPrice;

    @Schema(description = "金额（金额透明，不脱敏）", example = "2550.00")
    private BigDecimal amount;

}

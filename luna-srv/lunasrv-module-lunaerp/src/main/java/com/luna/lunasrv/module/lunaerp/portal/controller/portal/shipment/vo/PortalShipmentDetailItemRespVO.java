package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "客户门户 - 发货单明细项 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalShipmentDetailItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "201")
    private Long id;

    @Schema(description = "产品编码快照", example = "P-COTTON-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉斜纹面料")
    private String productName;

    @Schema(description = "客户色号", example = "COL-998")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "藏青色")
    private String colorName;

    @Schema(description = "缸号/批次号", example = "VAT-202610-01")
    private String vatNo;

    @Schema(description = "发货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "单价（透明展示）", example = "25.60")
    private BigDecimal unitPrice;

    @Schema(description = "行金额（透明展示）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12800.00")
    private BigDecimal amount;

    @Schema(description = "卷清单列表")
    private List<PortalShipmentRollRespVO> rolls;

}

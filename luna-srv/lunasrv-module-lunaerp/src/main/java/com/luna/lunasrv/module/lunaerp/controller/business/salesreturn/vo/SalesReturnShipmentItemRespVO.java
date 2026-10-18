package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货原发货明细行带可退量 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnShipmentItemRespVO {

    @Schema(description = "发货明细ID", example = "100")
    private Long shipmentItemId;

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

    @Schema(description = "单位（KG/M）", example = "KG")
    private String unit;

    @Schema(description = "原发货数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "原发货匹数", example = "2")
    private Integer pieces;

    @Schema(description = "原发货单价（元）", example = "25.0000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unitPrice;

    @Schema(description = "累计已占退货量（审批中+已审批）", example = "30.00")
    private BigDecimal occupiedQty;

    @Schema(description = "实时可退量（原发货数量 - 累计占量）", example = "70.00")
    private BigDecimal availableQty;

    @Schema(description = "原发货卷清单")
    private List<SalesReturnShipmentRollRespVO> rolls;

}

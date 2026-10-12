package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "客户门户 - 发货单详情 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalShipmentDetailRespVO {

    @Schema(description = "发货单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long id;

    @Schema(description = "发货单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SH-20261011-001")
    private String shipmentNo;

    @Schema(description = "发货日期", example = "2026-10-11")
    private LocalDate shippingDate;

    @Schema(description = "承运商/物流公司", example = "顺丰速运")
    private String logisticsCompany;

    @Schema(description = "物流单号", example = "SF1234567890")
    private String logisticsTrackingNo;

    @Schema(description = "签收状态（PENDING 待签收 / SIGNED 已签收 / REFUSED 拒签）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING")
    private String signStatus;

    @Schema(description = "签收人姓名", example = "张经理")
    private String signedByName;

    @Schema(description = "签收时间", example = "2026-10-11 15:30:00")
    private LocalDateTime signedAt;

    @Schema(description = "签收备注", example = "包装完好，如数收讫")
    private String signRemark;

    @Schema(description = "货物金额合计（透明展示）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12800.50")
    private BigDecimal totalAmount;

    @Schema(description = "运费（透明展示）", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    private BigDecimal freightFee;

    @Schema(description = "总匹数", example = "20")
    private Integer totalPieces;

    @Schema(description = "发货明细列表")
    private List<PortalShipmentDetailItemRespVO> items;

}

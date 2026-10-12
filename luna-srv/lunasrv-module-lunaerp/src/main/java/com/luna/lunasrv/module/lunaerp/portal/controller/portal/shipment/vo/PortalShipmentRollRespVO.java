package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "客户门户 - 发货单卷明细 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalShipmentRollRespVO {

    @Schema(description = "卷记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "卷号", example = "R001")
    private String rollNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数", example = "100.00")
    private BigDecimal lengthM;

}

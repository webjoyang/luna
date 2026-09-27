package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 业务管理 - 发货卷清单 Response VO")
@Data
public class ShipmentRollRespVO {

    @Schema(description = "卷记录ID", example = "1001")
    private Long id;

    @Schema(description = "所属发货单ID", example = "10")
    private Long shipmentId;

    @Schema(description = "所属发货明细ID", example = "100")
    private Long shipmentItemId;

    @Schema(description = "卷号", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数", example = "50.00")
    private BigDecimal lengthM;

    @Schema(description = "卷备注", example = "A等品")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理 - 发货卷清单创建/修改 Request VO")
@Data
public class ShipmentRollSaveReqVO {

    @Schema(description = "卷记录ID", example = "1001")
    private Long id;

    @Schema(description = "卷号", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数", example = "50.00")
    private BigDecimal lengthM;

    @Schema(description = "卷备注", example = "A等品")
    private String remark;

}

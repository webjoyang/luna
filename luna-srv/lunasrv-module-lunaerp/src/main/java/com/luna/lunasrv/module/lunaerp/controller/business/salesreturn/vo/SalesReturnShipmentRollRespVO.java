package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理 - 销售退货原发货卷清单 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnShipmentRollRespVO {

    @Schema(description = "原发货卷记录ID", example = "1001")
    private Long shipmentRollId;

    @Schema(description = "原卷号", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "实称重量（KG）", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数（M）", example = "50.00")
    private BigDecimal lengthM;

    @Schema(description = "卷备注", example = "A等品")
    private String remark;

}

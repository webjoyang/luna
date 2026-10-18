package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 业务管理 - 销售退货卷明细 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnRollRespVO {

    @Schema(description = "退货卷ID", example = "1001")
    private Long id;

    @Schema(description = "所属退货单ID", example = "10")
    private Long returnId;

    @Schema(description = "所属退货明细ID", example = "101")
    private Long returnItemId;

    @Schema(description = "关联原发货卷ID（可空）", example = "5001")
    private Long shipmentRollId;

    @Schema(description = "卷号", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "实称重量（KG）", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数（M）", example = "50.00")
    private BigDecimal lengthM;

    @Schema(description = "质检结果判定（RESTOCK/SCRAP）", example = "RESTOCK")
    private String qcResult;

    @Schema(description = "质检判定人ID", example = "1")
    private Long qcBy;

    @Schema(description = "质检判定时间")
    private LocalDateTime qcAt;

    @Schema(description = "质检处置备注", example = "表面轻微污渍可回洗入库")
    private String qcRemark;

    @Schema(description = "目标库位ID", example = "6001")
    private Long locationId;

    @Schema(description = "目标库位编码快照", example = "A-01-01")
    private String locationCode;

    @Schema(description = "备注", example = "A等品")
    private String remark;

}

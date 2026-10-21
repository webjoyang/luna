package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车间移动 H5 - 胚布扫码定位编排出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 胚布扫码定位编排出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketScanRespVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inspectionOrderId;

    @Schema(description = "关联织造布票ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long weavingTicketId;

    @Schema(description = "建议匹号（缺省为布票编号）", example = "BT-MTZZPB09190010101-0001")
    private String suggestedPieceNo;

    @Schema(description = "已录检明细行（未录检时为 null，存在时供改录）")
    private WorkshopInspectionItemRespVO item;

}

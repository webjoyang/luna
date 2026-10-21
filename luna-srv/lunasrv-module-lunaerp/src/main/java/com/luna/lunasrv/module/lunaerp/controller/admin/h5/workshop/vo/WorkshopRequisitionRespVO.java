package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车间移动 H5 - 待领纱单精简出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 待领纱单精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopRequisitionRespVO {

    @Schema(description = "领纱单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long id;

    @Schema(description = "领纱单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "YS-ZZPB091900101-01")
    private String code;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "201")
    private Long weavingOrderId;

    @Schema(description = "关联织造单号快照", example = "ZZ-PB2026092601-01")
    private String weavingOrderCode;

    @Schema(description = "领纱单状态（ACTIVE待发纱 / PARTIAL部分发纱）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "备注", example = "生产急用")
    private String remark;

    @Schema(description = "创建时间", example = "2026-10-21T10:00:00")
    private LocalDateTime createTime;

}

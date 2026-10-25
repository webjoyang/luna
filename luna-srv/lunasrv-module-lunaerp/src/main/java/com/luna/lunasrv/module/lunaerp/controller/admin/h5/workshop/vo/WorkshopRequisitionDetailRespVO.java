package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车间移动 H5 - 领纱单详情精简出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 领纱单详情精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopRequisitionDetailRespVO {

    @Schema(description = "领纱单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long id;

    @Schema(description = "领纱单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "YS-ZZPB091900101-01")
    private String code;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "201")
    private Long weavingOrderId;

    @Schema(description = "关联织造单号快照", example = "ZZ-PB2026092601-01")
    private String weavingOrderCode;

    @Schema(description = "领纱单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "备注", example = "车间生产急用")
    private String remark;

    @Schema(description = "创建时间", example = "2026-10-21T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "默认仓库ID（从系统参数 luna.workshop.default-warehouse-id 读取，若未配置或未启用则为 null）", example = "1")
    private Long defaultWarehouseId;

    @Schema(description = "领纱明细列表")
    private List<WorkshopRequisitionItemRespVO> items;

}

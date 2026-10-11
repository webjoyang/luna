package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 排产待排池 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPoolRespVO {

    @Schema(description = "组一：无日期待排任务列表（status=PENDING 且起止日期皆空）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PendingTaskVO> unDatedTasks;

    @Schema(description = "组二：未排满织造单列表（DRAFT/ACTIVE 且 remainingQty > 0）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PendingOrderVO> unScheduledOrders;

}

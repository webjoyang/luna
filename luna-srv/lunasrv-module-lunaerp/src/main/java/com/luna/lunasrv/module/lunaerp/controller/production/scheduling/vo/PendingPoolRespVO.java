package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 待排池 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPoolRespVO {

    @Schema(description = "组一：未排期任务列表（status=PENDING 且无日期）")
    private List<PendingTaskVO> unDatedTasks;

    @Schema(description = "组二：未排满织造单列表（DRAFT/ACTIVE 且 remainingQty > 0）")
    private List<PendingOrderVO> unScheduledOrders;

}

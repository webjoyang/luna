package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理 - 待排池未排满织造单 Response VO")
@Data
public class PendingOrderVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Long weavingOrderId;

    @Schema(description = "织造单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ2026100702")
    private String weavingOrderCode;

    @Schema(description = "产品编号快照", example = "P002")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉斜纹")
    private String productName;

    @Schema(description = "织造单计划匹数", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    private Integer plannedQty;

    @Schema(description = "已排产匹数（占用态任务计划匹数合计）", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer scheduledQty;

    @Schema(description = "剩余待排匹数（plannedQty - scheduledQty）", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer remainingQty;

}

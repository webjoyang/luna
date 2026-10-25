package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理 - 待排池 未排满织造单 VO")
@Data
public class PendingOrderVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Long weavingOrderId;

    @Schema(description = "织造单号", example = "ZZ-PB0919001-02")
    private String weavingOrderCode;

    @Schema(description = "产品编号", example = "P002")
    private String productCode;

    @Schema(description = "产品名称", example = "双面卫衣布")
    private String productName;

    @Schema(description = "织造单计划匹数", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    private Integer plannedQty;

    @Schema(description = "已排产匹数（占用态任务计划匹数合计）", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer scheduledQty;

    @Schema(description = "剩余待排匹数（plannedQty - scheduledQty）", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer remainingQty;

}

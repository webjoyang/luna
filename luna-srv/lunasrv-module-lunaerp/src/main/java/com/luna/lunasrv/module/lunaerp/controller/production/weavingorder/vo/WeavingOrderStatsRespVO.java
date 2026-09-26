package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 生产管理 - 织造单统计数据 Response VO")
@Data
public class WeavingOrderStatsRespVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long weavingOrderId;

    @Schema(description = "织造单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ-PB0919001-01")
    private String orderCode;

    @Schema(description = "计划匹数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer plannedQty;

    @Schema(description = "计划重量(KG)", example = "2500.00")
    private BigDecimal plannedWeight;

    @Schema(description = "完工匹数", requiredMode = Schema.RequiredMode.REQUIRED, example = "80")
    private Integer completedQty;

    @Schema(description = "总票数（含作废）", requiredMode = Schema.RequiredMode.REQUIRED, example = "85")
    private Integer totalTickets;

    @Schema(description = "有效票数（排除作废）", requiredMode = Schema.RequiredMode.REQUIRED, example = "80")
    private Integer validTickets;

    @Schema(description = "作废票数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer voidTickets;

    @Schema(description = "完工票数（COMPLETED/INSPECTED/STOCKED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "60")
    private Integer completedTickets;

    @Schema(description = "在产票数（STARTED/EXCEPTION）", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Integer inProgressTickets;

    @Schema(description = "待开工票数（GENERATED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer generatedTickets;

    @Schema(description = "完成率（百分比，0-100）", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.0")
    private Double completionRate;

    @Schema(description = "各状态布票数量计数映射")
    private Map<String, Integer> statusCounts;

    @Schema(description = "机台任务明细统计")
    private List<MachineTaskProgressVO> taskStats;

    @Schema(description = "机台任务明细统计 VO")
    @Data
    public static class MachineTaskProgressVO {
        @Schema(description = "机台任务ID", example = "10")
        private Long taskId;

        @Schema(description = "机台任务编号", example = "MT-ZZPB091900101-01")
        private String taskCode;

        @Schema(description = "织机ID", example = "1")
        private Long loomId;

        @Schema(description = "织机号", example = "L001")
        private String loomNo;

        @Schema(description = "挡车工", example = "张三")
        private String workerName;

        @Schema(description = "计划匹数", example = "50")
        private Integer plannedQty;

        @Schema(description = "实织/完工匹数", example = "40")
        private Integer actualQty;

        @Schema(description = "任务状态", example = "IN_PROGRESS")
        private String status;

        @Schema(description = "任务完成率（百分比）", example = "80.0")
        private Double taskCompletionRate;
    }
}

package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理 - 机台任务 Response VO")
@Data
public class MachineTaskRespVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MT-ZZPB091900101-01")
    private String code;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long weavingOrderId;

    @Schema(description = "关联织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long loomId;

    @Schema(description = "机台号快照", example = "LM20260001")
    private String loomNo;

    @Schema(description = "挡车工用户ID", example = "1")
    private Long workerUserId;

    @Schema(description = "挡车工姓名快照", example = "张三")
    private String workerName;

    @Schema(description = "计划匹数（正整数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    private Integer plannedQty;

    @Schema(description = "完工/实织匹数（布票聚合只读）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer actualQty;

    @Schema(description = "计划开始日期", example = "2026-09-26")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期", example = "2026-09-28")
    private LocalDate plannedEnd;

    @Schema(description = "任务状态（字典 lunaerp_weaving_task_status：PENDING/IN_PROGRESS/HALTED/COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING")
    private String status;

    @Schema(description = "停机原因", example = "机械故障检修")
    private String haltReason;

    @Schema(description = "停机时间", example = "2026-09-26 14:00:00")
    private LocalDateTime haltAt;

    @Schema(description = "超量软警告信息（超织造单计划匹数时返回）", example = "单内机台任务计划匹数合计(120匹)超出织造单计划匹数(100匹)")
    private String warning;

    @Schema(description = "备注", example = "车间备注")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}

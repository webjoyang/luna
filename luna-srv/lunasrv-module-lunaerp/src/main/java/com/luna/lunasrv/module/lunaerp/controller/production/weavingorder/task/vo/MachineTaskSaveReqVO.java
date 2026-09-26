package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 生产管理 - 机台任务创建/修改 Request VO")
@Data
public class MachineTaskSaveReqVO {

    @Schema(description = "机台任务ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "机台任务编号（留空时后端自动生成 MT-{织造单号去符号}-{2位序号}）", example = "MT-ZZPB091900101-01")
    @Size(max = 64, message = "任务编号长度不能超过 64 位")
    private String code;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联织造单不能为空")
    private Long weavingOrderId;

    @Schema(description = "关联织机ID（仅 ACTIVE 织机可选）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "织机不能为空")
    private Long loomId;

    @Schema(description = "挡车工用户ID（关联 system User，可空，ADR-0021）", example = "1")
    private Long workerUserId;

    @Schema(description = "挡车工姓名快照", example = "张三")
    @Size(max = 64, message = "挡车工姓名长度不能超过 64 位")
    private String workerName;

    @Schema(description = "计划匹数（正整数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    @NotNull(message = "计划匹数不能为空")
    private Integer plannedQty;

    @Schema(description = "计划开始日期（普通日期可空）", example = "2026-09-26")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期（普通日期可空，end ≥ start）", example = "2026-09-28")
    private LocalDate plannedEnd;

    @Schema(description = "备注", example = "加急排产")
    @Size(max = 500, message = "备注长度不能超过 500 位")
    private String remark;

}

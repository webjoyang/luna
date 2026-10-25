package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 生产管理 - 机台任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MachineTaskPageReqVO extends PageParam {

    @Schema(description = "关联织造单ID", example = "1024")
    private Long weavingOrderId;

    @Schema(description = "关联织机ID", example = "2048")
    private Long loomId;

    @Schema(description = "关联织机ID集合", example = "[2048, 2049]")
    private java.util.List<Long> loomIds;

    @Schema(description = "挡车工用户ID", example = "1")
    private Long workerUserId;

    @Schema(description = "任务状态（字典 lunaerp_weaving_task_status）", example = "PENDING")
    private String status;

    @Schema(description = "任务状态集合", example = "[\"PENDING\", \"IN_PROGRESS\"]")
    private java.util.List<String> statuses;

    @Schema(description = "任务编号关键词", example = "MT-ZZ")
    private String code;

    @Schema(description = "是否仅看冲突任务", example = "true")
    private Boolean conflictOnly;

    @Schema(description = "计划开始日期上限/过滤起", example = "2026-10-01")
    @org.springframework.format.annotation.DateTimeFormat(pattern = com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY)
    private java.time.LocalDate startDate;

    @Schema(description = "计划结束日期下限/过滤止", example = "2026-10-31")
    @org.springframework.format.annotation.DateTimeFormat(pattern = com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY)
    private java.time.LocalDate endDate;

}

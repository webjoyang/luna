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

    @Schema(description = "挡车工用户ID", example = "1")
    private Long workerUserId;

    @Schema(description = "任务状态（字典 lunaerp_weaving_task_status）", example = "PENDING")
    private String status;

    @Schema(description = "任务编号关键词", example = "MT-ZZ")
    private String code;

}

package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 车间移动 H5 - 我的机台任务分页 Request VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 我的机台任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkshopMyTasksPageReqVO extends PageParam {

    @Schema(description = "任务状态（可选，缺省默认 PENDING,IN_PROGRESS,HALTED；支持单值或逗号分隔）", example = "IN_PROGRESS")
    private String status;

}

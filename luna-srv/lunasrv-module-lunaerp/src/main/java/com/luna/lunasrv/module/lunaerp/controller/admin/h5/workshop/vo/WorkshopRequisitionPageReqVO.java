package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 车间移动 H5 - 待领纱单分页查询入参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 待领纱单分页查询入参 VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkshopRequisitionPageReqVO extends PageParam {

    @Schema(description = "单据状态过滤，多个逗号分隔，默认 ACTIVE,PARTIAL，DISPATCHED/CLOSED/DRAFT 不列", example = "ACTIVE,PARTIAL")
    private String status;

}

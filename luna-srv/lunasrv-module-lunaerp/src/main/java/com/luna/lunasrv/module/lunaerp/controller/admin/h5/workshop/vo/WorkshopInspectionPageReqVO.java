package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 车间移动 H5 - 移动质检单分页 Request VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 移动质检单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkshopInspectionPageReqVO extends PageParam {

    @Schema(description = "检验单状态（可选，缺省默认 DRAFT,ACTIVE；支持单值或逗号分隔）", example = "ACTIVE")
    private String status;

    @Schema(description = "检验类型（可选，GREIGE 胚布检验 / FINISHED 成品检验）", example = "GREIGE")
    private String type;

}

package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车间移动 H5 - 启用中仓库精简 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 启用中仓库精简 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopWarehouseSimpleRespVO {

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "车间线纱仓")
    private String name;

}

package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理-检验单录检明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionOrderItemPageReqVO extends PageParam {

    @Schema(description = "所属检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "所属检验单不能为空")
    private Long inspectionOrderId;

}

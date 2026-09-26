package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-领纱测算 Request VO")
@Data
public class YarnRequisitionCalculateReqVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "织造单ID不能为空")
    private Long weavingOrderId;

}

package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 领纱明细撤销发纱（红冲） Request VO")
@Data
public class YarnRequisitionDispatchReversalReqVO {

    @Schema(description = "发纱库存流水ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "发纱记录ID不能为空")
    private Long stockLedgerId;

    @Schema(description = "撤销备注说明", example = "误发撤销")
    private String remark;

}

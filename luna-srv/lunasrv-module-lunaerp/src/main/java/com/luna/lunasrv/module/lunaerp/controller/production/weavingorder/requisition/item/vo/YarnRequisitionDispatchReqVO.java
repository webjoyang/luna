package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 领纱明细发纱 Request VO")
@Data
public class YarnRequisitionDispatchReqVO {

    @Schema(description = "领纱明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "领纱明细ID不能为空")
    private Long itemId;

    @Schema(description = "发纱仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发纱仓库不能为空")
    private Long warehouseId;

    @Schema(description = "库位ID（可空）", example = "10")
    private Long locationId;

    @Schema(description = "发纱数量(KG)", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @NotNull(message = "发纱数量不能为空")
    @DecimalMin(value = "0.01", message = "发纱数量必须大于0")
    private BigDecimal dispatchQty;

    @Schema(description = "备注说明", example = "首次发纱")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 检验单生成胚布入库单 Request VO")
@Data
public class InspectionOrderGenerateStockInReqVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "检验单ID不能为空")
    private Long inspectionOrderId;

    @Schema(description = "入库仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    @Schema(description = "备注", example = "检验合格自动生成胚布入库单")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-领纱测算明细行 Response VO")
@Data
public class YarnRequisitionCalculateItemRespVO {

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "配比百分比数字（49 = 49%）", example = "49.00")
    private BigDecimal ratio;

    @Schema(description = "需求量 KG（测算结果）", requiredMode = Schema.RequiredMode.REQUIRED, example = "122.50")
    private BigDecimal requiredQty;

    @Schema(description = "单位", example = "KG")
    private String unit = "KG";

    @Schema(description = "备注", example = "BOM 自动测算")
    private String remark;

}

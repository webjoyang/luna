package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-领纱明细创建/修改 Request VO")
@Data
public class YarnRequisitionItemSaveReqVO {

    @Schema(description = "明细ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "所属领纱单ID", example = "100")
    private Long requisitionId;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "纱线规格不能为空")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称快照（为空由系统自动回填）", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "需求量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120.50")
    @NotNull(message = "需求量不能为空")
    @DecimalMin(value = "0.01", message = "需求量必须大于0")
    private BigDecimal requiredQty;

    @Schema(description = "单位，缺省 KG", example = "KG")
    private String unit = "KG";

    @Schema(description = "备注", example = "经纱领用")
    private String remark;

}

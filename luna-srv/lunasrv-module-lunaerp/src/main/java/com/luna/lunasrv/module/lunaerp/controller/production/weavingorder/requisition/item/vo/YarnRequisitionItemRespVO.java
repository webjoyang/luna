package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-领纱明细 Response VO")
@Data
public class YarnRequisitionItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属领纱单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long requisitionId;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "需求量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120.50")
    private BigDecimal requiredQty;

    @Schema(description = "已发量（KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.00")
    private BigDecimal dispatchedQty;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "备注", example = "经纱领用")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 样品分析纱线规格明细 Response VO")
@Data
public class SampleAnalysisYarnSpecRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long id;

    @Schema(description = "样品分析ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long sampleAnalysisId;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long yarnSpecId;

    @Schema(description = "材质ID（快照）", example = "1")
    private Long yarnTypeId;

    @Schema(description = "材质名称（快照）", example = "棉")
    private String yarnTypeName;

    @Schema(description = "规格名称（快照）", example = "32S 棉")
    private String specName;

    @Schema(description = "纺法（快照）", example = "环锭纺")
    private String spinMethod;

    @Schema(description = "用纱比例（百分比，如 30 表示 30%）", example = "30.00")
    private BigDecimal ratio;

    @Schema(description = "单价（元/KG）", example = "25.50")
    private BigDecimal unitPrice;

    @Schema(description = "备注", example = "经纱用")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

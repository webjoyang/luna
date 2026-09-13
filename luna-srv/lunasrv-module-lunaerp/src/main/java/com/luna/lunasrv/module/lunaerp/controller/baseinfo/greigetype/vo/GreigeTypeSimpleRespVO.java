package com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 胚布品种精简 Response VO（产品表单选择器用）")
@Data
public class GreigeTypeSimpleRespVO {

    @Schema(description = "胚布品种ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "品种编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "GT-0001")
    private String code;

    @Schema(description = "显示名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "30S NR罗马布 260g×160cm")
    private String name;

    @Schema(description = "布种", requiredMode = Schema.RequiredMode.REQUIRED, example = "罗马布")
    private String fabricType;

    @Schema(description = "成分", requiredMode = Schema.RequiredMode.REQUIRED, example = "NR")
    private String composition;

    @Schema(description = "纱支", requiredMode = Schema.RequiredMode.REQUIRED, example = "30S")
    private String yarnSpec;

    @Schema(description = "坯克重（g/m²）", requiredMode = Schema.RequiredMode.REQUIRED, example = "260")
    private BigDecimal weightGsm;

    @Schema(description = "坯门幅（厘米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "160")
    private BigDecimal widthCm;

}

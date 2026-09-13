package com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 胚布品种信息 Response VO")
@Data
public class GreigeTypeRespVO {

    @Schema(description = "胚布品种ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "品种编号（租户内流水号，如 GT-0001）", requiredMode = Schema.RequiredMode.REQUIRED, example = "GT-0001")
    private String code;

    @Schema(description = "显示名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "30S NR罗马布 260g×160cm")
    private String name;

    @Schema(description = "识别键（服务端按五个识别属性生成，前端传值不被信任）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "罗马布|NR|30S|260|160")
    private String matchKey;

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

    @Schema(description = "工艺类型", example = "针织")
    private String craftType;

    @Schema(description = "机型", example = "34 寸大圆机")
    private String machineType;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

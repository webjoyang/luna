package com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 胚布品种创建/修改 Request VO")
@Data
public class GreigeTypeSaveReqVO {

    @Schema(description = "胚布品种编号（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "品种编号（不传时由服务端生成 GT-XXXX，租户内递增 4 位补零）", example = "GT-0001")
    private String code;

    @Schema(description = "显示名称（不传时由五个识别属性按规则生成，如 30S NR罗马布 260g×160cm）",
            example = "30S NR罗马布 260g×160cm")
    private String name;

    @Schema(description = "布种（取字典 FABRIC_TYPE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "罗马布")
    @NotBlank(message = "布种不能为空")
    private String fabricType;

    @Schema(description = "成分（如 NR / TR / CVC，服务端统一归一化为大写）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "NR")
    @NotBlank(message = "成分不能为空")
    private String composition;

    @Schema(description = "纱支（如 30S，服务端统一归一化为大写）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "30S")
    @NotBlank(message = "纱支不能为空")
    private String yarnSpec;

    @Schema(description = "坯克重（g/m²，注意与成品克重不同）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "260")
    @NotNull(message = "坯克重不能为空")
    @Positive(message = "坯克重必须大于 0")
    private BigDecimal weightGsm;

    @Schema(description = "坯门幅（厘米，注意与成品门幅不同）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "160")
    @NotNull(message = "坯门幅不能为空")
    @Positive(message = "坯门幅必须大于 0")
    private BigDecimal widthCm;

    @Schema(description = "工艺类型（选填）", example = "针织")
    private String craftType;

    @Schema(description = "机型（选填）", example = "34 寸大圆机")
    private String machineType;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），创建不传默认 ACTIVE", example = "ACTIVE")
    private String status;

}

package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 打色指示单颜色行保存 VO")
@Data
public class ColorInstructionColorSaveVO {

    @Schema(description = "颜色行ID（新建时空，差异更新时传）", example = "1024")
    private Long id;

    @Schema(description = "客户色号", requiredMode = Schema.RequiredMode.REQUIRED, example = "C01")
    @NotBlank(message = "客户色号不能为空")
    private String customerColorNo;

    @Schema(description = "颜色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "藏青")
    @NotBlank(message = "颜色名称不能为空")
    private String colorName;

    @Schema(description = "厂内色号", example = "F-01")
    private String factoryColorNo;

    @Schema(description = "色值 #rrggbb", example = "#000080")
    private String colorHex;

    @Schema(description = "主灯源（字典 lunaerp_color_light_source）", requiredMode = Schema.RequiredMode.REQUIRED, example = "D65")
    @NotBlank(message = "主灯源不能为空")
    private String lightPrimary;

    @Schema(description = "辅灯源（字典 lunaerp_color_light_source）", example = "TL84")
    private String lightSecondary;

    @Schema(description = "色卡参考", example = "Pantone TPX 19-4052")
    private String colorCardRef;

    @Schema(description = "样布照片 JSON 数组/URL 列表")
    private List<String> samplePhotos;

    @Schema(description = "份数", example = "2")
    private Integer copies;

    @Schema(description = "打色轮次（缺省 1）", example = "1")
    private Integer round;

    @Schema(description = "行状态：PENDING/APPROVED/REJECTED（缺省 PENDING）", example = "PENDING")
    private String status;

    @Schema(description = "驳回/否决意见", example = "偏红")
    private String rejectReason;

    @Schema(description = "行备注", example = "注意手感")
    private String remark;
}

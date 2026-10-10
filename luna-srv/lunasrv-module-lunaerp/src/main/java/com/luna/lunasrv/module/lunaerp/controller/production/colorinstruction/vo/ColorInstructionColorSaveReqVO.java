package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理-打色指示单颜色行保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionColorSaveReqVO {

    @Schema(description = "颜色行ID（新建为 null，编辑为行ID）", example = "1024")
    private Long id;

    @Schema(description = "行号（服务端重排，新建可不传）", example = "1")
    private Integer lineNo;

    @Schema(description = "客户色号（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "C01")
    @NotBlank(message = "客户色号不能为空")
    @Size(max = 64, message = "客户色号长度不能超过 64 位")
    private String customerColorNo;

    @Schema(description = "颜色名称（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "大红")
    @NotBlank(message = "颜色名称不能为空")
    @Size(max = 64, message = "颜色名称长度不能超过 64 位")
    private String colorName;

    @Schema(description = "厂内色号", example = "F001")
    @Size(max = 64, message = "厂内色号长度不能超过 64 位")
    private String factoryColorNo;

    @Schema(description = "色值十六进制（如 #FF0000）", example = "#FF0000")
    @Size(max = 16, message = "色值长度不能超过 16 位")
    private String colorHex;

    @Schema(description = "主灯源（必填，字典 lunaerp_color_light_source）", requiredMode = Schema.RequiredMode.REQUIRED, example = "D65")
    @NotBlank(message = "主灯源不能为空")
    @Size(max = 32, message = "主灯源长度不能超过 32 位")
    private String lightPrimary;

    @Schema(description = "辅灯源（字典 lunaerp_color_light_source）", example = "TL84")
    @Size(max = 32, message = "辅灯源长度不能超过 32 位")
    private String lightSecondary;

    @Schema(description = "色卡参考（如 Pantone TPX 19-4052）", example = "Pantone TPX 19-4052")
    @Size(max = 128, message = "色卡参考长度不能超过 128 位")
    private String colorCardRef;

    @Schema(description = "样布照片 JSON 数组 [{\"name\":\"...\",\"url\":\"...\"}]")
    private String samplePhotos;

    @Schema(description = "份数", example = "2")
    private Integer copies;

    @Schema(description = "打色轮次（新建缺省 1）", example = "1")
    private Integer round;

    @Schema(description = "行状态（新建缺省 PENDING）", example = "PENDING")
    private String status;

    @Schema(description = "驳回意见")
    private String rejectReason;

    @Schema(description = "行备注")
    private String remark;

    @Schema(description = "是否删除（显式删除标记）")
    private Boolean deleted;

}

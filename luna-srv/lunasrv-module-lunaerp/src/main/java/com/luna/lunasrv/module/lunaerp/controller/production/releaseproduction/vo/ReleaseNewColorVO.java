package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 销售合同下达生产新建打色单颜色行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseNewColorVO {

    @Schema(description = "来源合同行ID", example = "1")
    private Long contractLineId;

    @Schema(description = "客户色号", example = "C101")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "大红")
    private String colorName;

    @Schema(description = "主灯源（必填，字典 lunaerp_color_light_source）", requiredMode = Schema.RequiredMode.REQUIRED, example = "D65")
    private String lightPrimary;

    @Schema(description = "厂内色号", example = "F001")
    private String factoryColorNo;

    @Schema(description = "色卡参考", example = "Pantone TPX 19-4052")
    private String colorCardRef;

}

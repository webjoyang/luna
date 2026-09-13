package com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 产品标签创建/修改 Request VO")
@Data
public class ProductTagSaveReqVO {

    @Schema(description = "标签ID（修改时必填）", example = "2048")
    private Long id;

    @Schema(description = "标签名称（同租户内同名共用颜色，最多 8 个字）", requiredMode = Schema.RequiredMode.REQUIRED, example = "主推")
    @NotBlank(message = "标签名称不能为空")
    private String name;

    @Schema(description = "标签颜色（预设色：camel/blue/teal/green/orange/pink/purple/graphite，缺省 graphite）", example = "graphite")
    private String color;

}

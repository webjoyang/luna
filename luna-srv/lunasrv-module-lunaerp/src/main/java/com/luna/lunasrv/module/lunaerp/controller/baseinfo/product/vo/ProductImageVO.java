package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 产品图片 VO")
@Data
public class ProductImageVO {

    @Schema(description = "图片ID（仅响应时返回）", example = "1024")
    private Long id;

    @Schema(description = "图片URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.abc.com/a.png")
    @NotBlank(message = "图片URL不能为空")
    private String url;

    @Schema(description = "排序序号（详情页展示顺序）", example = "0")
    private Integer sortOrder;

}

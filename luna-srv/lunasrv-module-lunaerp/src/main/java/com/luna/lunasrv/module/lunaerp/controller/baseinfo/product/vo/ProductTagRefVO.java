package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 产品标签引用 VO（产品写操作入参 / 产品详情出参）")
@Data
public class ProductTagRefVO {

    @Schema(description = "标签ID（仅响应时返回）", example = "2048")
    private Long id;

    @Schema(description = "标签名称（最多 8 个字，同租户同名复用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "主推")
    @NotBlank(message = "标签名称不能为空")
    private String name;

    @Schema(description = "标签颜色（预设色：camel/blue/teal/green/orange/pink/purple/graphite，缺省 graphite；已存在标签保留原色）",
            example = "graphite")
    private String color;

    @Schema(description = "在该产品上的排序序号（仅响应时返回，来自标签关联）", example = "0")
    private Integer sortOrder;

}

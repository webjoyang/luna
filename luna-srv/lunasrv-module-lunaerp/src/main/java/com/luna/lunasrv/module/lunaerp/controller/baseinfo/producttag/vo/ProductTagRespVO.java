package com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 产品标签 Response VO")
@Data
public class ProductTagRespVO {

    @Schema(description = "标签ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "主推")
    private String name;

    @Schema(description = "标签颜色", requiredMode = Schema.RequiredMode.REQUIRED, example = "graphite")
    private String color;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

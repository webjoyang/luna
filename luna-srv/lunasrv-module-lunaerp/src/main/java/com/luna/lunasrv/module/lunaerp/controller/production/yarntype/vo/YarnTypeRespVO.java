package com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 纱线材质 Response VO")
@Data
public class YarnTypeRespVO {

    @Schema(description = "材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "材质名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "棉")
    private String name;

    @Schema(description = "排序值", example = "1")
    private Integer sort;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

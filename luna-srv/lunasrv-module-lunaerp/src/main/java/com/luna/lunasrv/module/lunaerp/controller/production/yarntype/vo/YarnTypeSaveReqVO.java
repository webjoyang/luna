package com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 纱线材质创建/修改 Request VO")
@Data
public class YarnTypeSaveReqVO {

    @Schema(description = "材质ID", example = "1")
    private Long id;

    @Schema(description = "材质名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "棉")
    @NotBlank(message = "材质名称不能为空")
    @Size(max = 64, message = "材质名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "排序值", example = "1")
    private Integer sort;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}

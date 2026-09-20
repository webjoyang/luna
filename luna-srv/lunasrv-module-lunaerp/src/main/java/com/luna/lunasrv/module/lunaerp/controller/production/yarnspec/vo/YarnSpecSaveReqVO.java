package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 纱线规格创建/修改 Request VO")
@Data
public class YarnSpecSaveReqVO {

    @Schema(description = "规格ID", example = "10")
    private Long id;

    @Schema(description = "所属材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属材质不能为空")
    private Long yarnTypeId;

    @Schema(description = "规格名称（展示用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S 棉")
    @NotBlank(message = "规格名称不能为空")
    @Size(max = 128, message = "规格名称长度不能超过 128 个字符")
    private String specName;

    @Schema(description = "标准名称（租户内+材质内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S-COTTON")
    @NotBlank(message = "标准名称不能为空")
    @Size(max = 128, message = "标准名称长度不能超过 128 个字符")
    private String standardName;

    @Schema(description = "纺纱工艺（环锭纺/转杯纺/喷气纺等）", example = "环锭纺")
    @Size(max = 64, message = "纺纱工艺长度不能超过 64 个字符")
    private String spinMethod;

    @Schema(description = "光泽（半光/全光/消光等）", example = "半光")
    @Size(max = 64, message = "光泽长度不能超过 64 个字符")
    private String luster;

    @Schema(description = "参考单价", example = "25.5000")
    private BigDecimal refPrice;

    @Schema(description = "备注", example = "常用规格")
    @Size(max = 1000, message = "备注长度不能超过 1000 个字符")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}

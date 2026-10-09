package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 纱线规格 Response VO")
@Data
public class YarnSpecRespVO {

    @Schema(description = "规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long id;

    @Schema(description = "所属材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long yarnTypeId;

    @Schema(description = "所属材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "规格名称（展示用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S 棉")
    private String specName;

    @Schema(description = "标准名称（租户内+材质内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S-COTTON")
    private String standardName;

    @Schema(description = "纺纱工艺（环锭纺/转杯纺/喷气纺等）", example = "环锭纺")
    private String spinMethod;

    @Schema(description = "光泽（半光/全光/消光等）", example = "半光")
    private String luster;

    @Schema(description = "参考单价", example = "25.5000")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal refPrice;

    @Schema(description = "备注", example = "常用规格")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

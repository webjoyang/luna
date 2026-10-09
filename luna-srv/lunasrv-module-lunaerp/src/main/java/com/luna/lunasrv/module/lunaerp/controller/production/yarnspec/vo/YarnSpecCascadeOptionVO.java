package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 纱线规格两级级联选项 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YarnSpecCascadeOptionVO {

    @Schema(description = "材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long yarnTypeId;

    @Schema(description = "材质名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "棉")
    private String yarnTypeName;

    @Schema(description = "该材质下的规格列表")
    private List<SpecOption> specs;

    @Schema(description = "管理后台 - 纱线规格选项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecOption {

        @Schema(description = "规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        private Long id;

        @Schema(description = "规格名称（展示用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S 棉")
        private String specName;

        @Schema(description = "标准名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S-COTTON")
        private String standardName;

        @Schema(description = "参考单价", example = "25.5000")
        @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
        private BigDecimal refPrice;

        @Schema(description = "纺纱工艺", example = "环锭纺")
        private String spinMethod;

        @Schema(description = "光泽", example = "半光")
        private String luster;

    }

}

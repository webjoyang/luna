package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseProductionReqVO {

    @Schema(description = "销售合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "销售合同不能为空")
    private Long contractId;

    @Schema(description = "承织坯布厂供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "承织坯布厂不能为空")
    private Long greigeFactoryId;

    @Schema(description = "承染方供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3073")
    @NotNull(message = "承染方不能为空")
    private Long dyeingFactoryId;

    @Schema(description = "下达分组列表（按同合同同产品分组）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "下达分组列表不能为空")
    @Valid
    private List<ReleaseGroupVO> groups;

}

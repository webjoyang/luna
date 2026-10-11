package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 销售合同下达生产检验单精简 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseInspectionSimpleVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "检验单号", example = "INS-20261011-001")
    private String code;

    @Schema(description = "检验类型（GREIGE 胚布检验 / FINISHED 成品检验）", example = "GREIGE")
    private String type;

}

package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 销售合同下达生产已有打色单 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseExistingColorInstructionVO {

    @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long id;

    @Schema(description = "打色单号", example = "DS20261007001")
    private String instructionNo;

    @Schema(description = "打色单状态", example = "CONFIRMED")
    private String status;

    @Schema(description = "产品编号", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉汗布")
    private String productName;

}

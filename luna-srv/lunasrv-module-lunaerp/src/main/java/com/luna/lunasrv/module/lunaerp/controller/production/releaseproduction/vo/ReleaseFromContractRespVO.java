package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产预填 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseFromContractRespVO {

    @Schema(description = "销售合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long contractId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CH20260001")
    private String contractNo;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某纺织")
    private String clientName;

    @Schema(description = "合同行明细列表")
    private List<ReleaseContractLineVO> lines;

    @Schema(description = "已有打色单列表")
    private List<ReleaseExistingColorInstructionVO> existingColorInstructions;

}

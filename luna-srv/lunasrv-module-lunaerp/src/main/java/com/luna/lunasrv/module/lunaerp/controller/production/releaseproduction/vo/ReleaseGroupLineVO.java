package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 销售合同下达生产分组明细行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseGroupLineVO {

    @Schema(description = "来源合同行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同行ID不能为空")
    private Long contractLineId;

    @Schema(description = "客户色号", example = "C101")
    private String customerColor;

    @Schema(description = "本次下达量 KG", requiredMode = Schema.RequiredMode.REQUIRED, example = "600.00")
    @NotNull(message = "本次下达量不能为空")
    private BigDecimal releaseQty;

    @Schema(description = "损耗率（缺省 0）", example = "0.05")
    private BigDecimal lossRate;

}

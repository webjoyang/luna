package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "成本卡费用明细行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardFeeItemVO {

    @Schema(description = "明细行ID", example = "100")
    private Long id;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "费用类型（WEAVING / DYEING / SETTING / INSPECTION / PACKAGING / FREIGHT）", requiredMode = Schema.RequiredMode.REQUIRED, example = "WEAVING")
    @NotBlank(message = "费用类型不能为空")
    private String feeType;

    @Schema(description = "计费单位（PER_KG / PER_METER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PER_KG")
    @NotBlank(message = "计费单位不能为空")
    private String feeUnit;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "5.0000")
    @NotNull(message = "单价不能为空")
    private BigDecimal price;

    @Schema(description = "投入损耗率%", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.00")
    @NotNull(message = "损耗率不能为空")
    private BigDecimal lossRate;

    @Schema(description = "行费用贡献 元/KG", example = "5.1020")
    private BigDecimal contributionPerKg;

    @Schema(description = "行费用贡献 元/M", example = "1.5306")
    private BigDecimal contributionPerMeter;

    @Schema(description = "备注", example = "织造工序")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "成本卡保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardSaveReqVO {

    @Schema(description = "成本卡ID（新增为空，修改必填）", example = "100")
    private Long id;

    @Schema(description = "成本卡号（留空自动生成 CC{yyyy}NNNN）", example = "CC20260001")
    private String cardNo;

    @Schema(description = "关联产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品不能为空")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P-1001")
    private String productCode;

    @Schema(description = "品名快照", example = "高弹针织汗布")
    private String productName;

    @Schema(description = "成分快照", example = "95%棉 5%氨纶")
    private String composition;

    @Schema(description = "门幅文本快照", example = "180CM")
    private String widthSnapshot;

    @Schema(description = "克重文本快照", example = "200GSM")
    private String weightSnapshot;

    @Schema(description = "织物结构（KNIT / WOVEN）", requiredMode = Schema.RequiredMode.REQUIRED, example = "KNIT")
    @NotBlank(message = "织物结构不能为空")
    private String fabricConstruction;

    @Schema(description = "梭织模式（SIMPLE / PROFESSIONAL）", example = "SIMPLE")
    private String wovenCalculationMode;

    @Schema(description = "取价价类（PURCHASE / MARKET）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PURCHASE")
    @NotBlank(message = "取价价类不能为空")
    private String yarnPriceType;

    @Schema(description = "坯布克重 g/m²", example = "200.00")
    private BigDecimal greigeWeightGsm;

    @Schema(description = "坯布门幅 CM", example = "180.00")
    private BigDecimal greigeWidthCm;

    @Schema(description = "每米重量 KG/M", example = "0.360000")
    private BigDecimal kgPerMeter;

    @Schema(description = "税率（如 0.13 表示 13%）", example = "0.130000")
    private BigDecimal taxRate;

    @Schema(description = "计费基准口径（KG / METER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    @NotBlank(message = "计费基准口径不能为空")
    private String basis;

    @Schema(description = "备注", example = "2026秋冬首期成本核算")
    private String remarks;

    @Schema(description = "纱线明细行列表")
    @Valid
    private List<CostCardYarnItemVO> yarnItems;

    @Schema(description = "费用明细行列表")
    @Valid
    private List<CostCardFeeItemVO> feeItems;

}

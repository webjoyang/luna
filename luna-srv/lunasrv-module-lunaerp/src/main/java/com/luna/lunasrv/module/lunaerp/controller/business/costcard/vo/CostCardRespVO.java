package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "成本卡 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCardRespVO {

    @Schema(description = "成本卡ID", example = "100")
    private Long id;

    @Schema(description = "成本卡号", example = "CC20260001")
    private String cardNo;

    @Schema(description = "关联产品ID", example = "1")
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

    @Schema(description = "织物结构（KNIT / WOVEN）", example = "KNIT")
    private String fabricConstruction;

    @Schema(description = "梭织模式（SIMPLE / PROFESSIONAL）", example = "SIMPLE")
    private String wovenCalculationMode;

    @Schema(description = "取价价类（PURCHASE / MARKET）", example = "PURCHASE")
    private String yarnPriceType;

    @Schema(description = "坯布克重 g/m²", example = "200.00")
    private BigDecimal greigeWeightGsm;

    @Schema(description = "坯布门幅 CM", example = "180.00")
    private BigDecimal greigeWidthCm;

    @Schema(description = "每米重量 KG/M", example = "0.360000")
    private BigDecimal kgPerMeter;

    @Schema(description = "税率", example = "0.130000")
    private BigDecimal taxRate;

    @Schema(description = "计费基准口径（KG / METER）", example = "KG")
    private String basis;

    @Schema(description = "纱线成本 元/KG", example = "35.5000")
    private BigDecimal yarnCostPerKg;

    @Schema(description = "纱线成本 元/M", example = "10.6500")
    private BigDecimal yarnCostPerMeter;

    @Schema(description = "费用合计 元/KG", example = "8.0000")
    private BigDecimal totalFeePerKg;

    @Schema(description = "费用合计 元/M", example = "2.4000")
    private BigDecimal totalFeePerMeter;

    @Schema(description = "不含税单位成本 元/KG", example = "43.5000")
    private BigDecimal unitCostPerKg;

    @Schema(description = "含税单位成本 元/KG", example = "49.1550")
    private BigDecimal unitCostPerKgWithTax;

    @Schema(description = "不含税单位成本 元/M", example = "13.0500")
    private BigDecimal unitCostPerMeter;

    @Schema(description = "含税单位成本 元/M", example = "14.7465")
    private BigDecimal unitCostPerMeterWithTax;

    @Schema(description = "缺价纱线行数", example = "0")
    private Integer missingPriceCount;

    @Schema(description = "生效日期", example = "2026-10-10")
    private LocalDate effectiveDate;

    @Schema(description = "状态（DRAFT / EFFECTIVE / EXPIRED）", example = "DRAFT")
    private String status;

    @Schema(description = "备注", example = "核算备注")
    private String remarks;

    @Schema(description = "纱线明细行列表")
    private List<CostCardYarnItemVO> yarnItems;

    @Schema(description = "费用明细行列表")
    private List<CostCardFeeItemVO> feeItems;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

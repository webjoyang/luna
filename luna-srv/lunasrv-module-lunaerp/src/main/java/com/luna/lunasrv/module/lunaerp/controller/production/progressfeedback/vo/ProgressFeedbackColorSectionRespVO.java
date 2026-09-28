package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-进度反馈颜色行骨架节 Response VO")
@Data
public class ProgressFeedbackColorSectionRespVO {

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long dyeingOrderId;

    @Schema(description = "销售合同ID", example = "10")
    private Long salesContractId;

    @Schema(description = "销售合同号（实时关联）", example = "HT2026-001")
    private String contractNo;

    @Schema(description = "客户ID", example = "20")
    private Long customerId;

    @Schema(description = "客户名称（实时关联）", example = "某某服装公司")
    private String clientName;

    @Schema(description = "承染方供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Long dyeingFactoryId;

    @Schema(description = "承染方公司名（实时关联）", example = "某某染织厂")
    private String dyeingFactoryName;

    @Schema(description = "染色单交货日期（实时关联）", example = "2026-10-15")
    private LocalDate deliveryDate;

    @Schema(description = "产品编码（实时关联）", example = "PRD001")
    private String productCode;

    @Schema(description = "产品名称（实时关联）", example = "精梳全棉斜纹面料")
    private String productName;

    @Schema(description = "染色单状态", example = "ACTIVE")
    private String dyeingOrderStatus;

    @Schema(description = "染色颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Long dyeingColorId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "客户色号", example = "PANTONE 19-4052")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "经典蓝")
    private String colorName;

    @Schema(description = "本单染色数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal colorQuantity;

    @Schema(description = "匹数", example = "40")
    private Integer colorPieces;

    @Schema(description = "单位（跟随颜色行单位）", example = "KG")
    private String unit;

    @Schema(description = "已建缸数量合计", requiredMode = Schema.RequiredMode.REQUIRED, example = "800.00")
    private BigDecimal totalVatQuantity;

    @Schema(description = "已建缸匹数合计", example = "32")
    private Integer totalVatPieces;

    @Schema(description = "排产覆盖率（%）", requiredMode = Schema.RequiredMode.REQUIRED, example = "80.00")
    private BigDecimal coveragePercent;

    @Schema(description = "是否超排（Σ缸数量 > 颜色行数量）", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean overScheduled;

    @Schema(description = "状态分组计数（键为状态值，值为该状态下的缸数）", example = "{\"SCHEDULED\": 1, \"DYEING\": 2}")
    private java.util.Map<String, Integer> statusCountMap;

    @Schema(description = "反馈缸列表（零缸时为空列表，前端渲染虚线未排缸占位卡）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProgressFeedbackVatRespVO> vats;

}

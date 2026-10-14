package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-染色单颜色明细行 Response VO")
@Data
public class DyeingColorRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long dyeingOrderId;

    @Schema(description = "行号（从 1 开始，按 line_no 升序返回）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

    @Schema(description = "来源合同行 ID", example = "2048")
    private Long contractLineId;

    @Schema(description = "来源合同行号快照", example = "1")
    private Integer contractLineNo;

    @Schema(description = "客户色号", example = "C-001")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "藏青")
    private String colorName;

    @Schema(description = "本单染色数量（成品口径）", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "投染数（含损耗的实际投入量）", example = "525.00")
    private BigDecimal dyeingInputQty;

    @Schema(description = "投染数单位（字典 lunaerp_sales_contract_unit：KG/M）", example = "KG")
    private String dyeingInputUnit;

    @Schema(description = "合同数量（从合同明细带出，只读对比）", example = "500.00")
    private BigDecimal contractQty;

}

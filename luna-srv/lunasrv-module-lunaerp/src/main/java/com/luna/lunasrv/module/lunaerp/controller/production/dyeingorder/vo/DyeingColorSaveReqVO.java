package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-染色单颜色明细行创建/修改 Request VO")
@Data
public class DyeingColorSaveReqVO {

    @Schema(description = "客户色号（从合同明细带出可改）", example = "C-001")
    @Size(max = 64, message = "客户色号长度不能超过 64 位")
    private String customerColorNo;

    @Schema(description = "颜色名称（手填）", example = "藏青")
    @Size(max = 128, message = "颜色名称长度不能超过 128 位")
    private String colorName;

    @Schema(description = "本单染色数量（成品口径）", example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "投染数（含损耗的实际投入量）", example = "525.00")
    private BigDecimal dyeingInputQty;

    @Schema(description = "投染数单位（字典 lunaerp_sales_contract_unit：KG/M）", example = "KG")
    @Size(max = 16, message = "投染数单位长度不能超过 16 位")
    private String dyeingInputUnit;

    @Schema(description = "合同数量（从合同明细带出，只读对比）", example = "500.00")
    private BigDecimal contractQty;

}

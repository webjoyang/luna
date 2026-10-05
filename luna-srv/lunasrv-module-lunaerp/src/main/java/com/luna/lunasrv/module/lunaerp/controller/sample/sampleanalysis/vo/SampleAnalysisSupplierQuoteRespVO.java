package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 样品管理-样品分析供应商报价 Response VO")
@Data
public class SampleAnalysisSupplierQuoteRespVO {

    @Schema(description = "供应商报价行ID", example = "1024")
    private Long id;

    @Schema(description = "样品分析ID", example = "100")
    private Long sampleAnalysisId;

    @Schema(description = "供应商ID（可空）", example = "200")
    private Long supplierId;

    @Schema(description = "供应商名称快照", example = "杭州某某针织印染有限公司")
    private String supplierName;

    @Schema(description = "胚布报价（元/KG，仅供参考不计入总成本）", example = "35.50")
    private BigDecimal greigePrice;

    @Schema(description = "备注", example = "含税现结")
    private String remark;

}

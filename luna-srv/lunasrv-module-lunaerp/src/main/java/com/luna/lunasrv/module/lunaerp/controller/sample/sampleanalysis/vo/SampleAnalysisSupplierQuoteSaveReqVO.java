package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 样品管理-样品分析供应商报价创建/修改 Request VO")
@Data
public class SampleAnalysisSupplierQuoteSaveReqVO {

    @Schema(description = "供应商报价行ID（更新时传入存量行ID，新增行留空）", example = "1024")
    private Long id;

    @Schema(description = "供应商ID（可空，逻辑关联供应商档案）", example = "200")
    private Long supplierId;

    @Schema(description = "供应商名称快照（可手填，或选供应商档案时自动带出）", example = "杭州某某针织印染有限公司")
    private String supplierName;

    @Schema(description = "胚布报价（元/KG，可空，仅供参考不计入总成本）", example = "35.50")
    private BigDecimal greigePrice;

    @Schema(description = "备注", example = "含税现结")
    private String remark;

}

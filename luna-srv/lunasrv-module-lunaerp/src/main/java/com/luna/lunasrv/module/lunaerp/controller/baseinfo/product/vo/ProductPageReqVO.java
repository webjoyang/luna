package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 基本信息-产品分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageReqVO extends PageParam {

    @Schema(description = "智能搜索词（对编号/名称/原厂编号/成分/类型分词后 AND 匹配）", example = "莫代尔汗布")
    private String search;

    @Schema(description = "产品编号，模糊匹配", example = "P0001")
    private String code;

    @Schema(description = "产品名称，模糊匹配", example = "莫代尔")
    private String name;

    @Schema(description = "成分，按分隔符分词后每词 AND 模糊匹配", example = "NR/SP")
    private String composition;

    @Schema(description = "产品类型，模糊匹配", example = "汗布")
    private String type;

    @Schema(description = "克重（单值，按 ±10g/m² 区间匹配；与 minWeight/maxWeight 互斥）", example = "260")
    private BigDecimal weightGsm;

    @Schema(description = "最小克重（g/m²）", example = "200")
    private BigDecimal minWeight;

    @Schema(description = "最大克重（g/m²）", example = "300")
    private BigDecimal maxWeight;

    @Schema(description = "状态：DRAFT 仅查草稿；其他值一律查 ACTIVE", example = "ACTIVE")
    private String status;

}

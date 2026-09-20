package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线采购测算明细行 Response VO")
@Data
public class YarnOrderCalculateItemRespVO {

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "纱线规格ID", example = "2")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "供应商ID", example = "3")
    private Long supplierId;

    @Schema(description = "供应商公司名快照", example = "常州某某纺织厂")
    private String supplierName;

    @Schema(description = "配比百分比数字（49 = 49%）", example = "49.00")
    private BigDecimal ratio;

    @Schema(description = "基础需求量", example = "490.00")
    private BigDecimal baseQty;

    @Schema(description = "损耗率百分比数字（5 = 5%）", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "含损耗需求量", example = "514.50")
    private BigDecimal requiredQty;

    @Schema(description = "在库量快照", example = "100.00")
    private BigDecimal stockQty;

    @Schema(description = "实采量", example = "414.50")
    private BigDecimal purchaseQty;

    @Schema(description = "历史价回填", example = "25.0000")
    private BigDecimal refUnitPrice;

    @Schema(description = "该规格涉及的胚布单 ID 列表", example = "[101, 102]")
    private List<Long> sourceGreigeOrderIds;

    @Schema(description = "该规格涉及的胚布单号列表", example = "[\"PB0920001\", \"PB0920002\"]")
    private List<String> sourceGreigeOrderNos;

}

package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-纱线采购明细 Response VO")
@Data
public class YarnOrderItemRespVO {

    @Schema(description = "明细ID", example = "1024")
    private Long id;

    @Schema(description = "所属采购单ID", example = "100")
    private Long orderId;

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

    @Schema(description = "损耗率百分比数字（5 = 5%）", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "基础需求量", example = "100.00")
    private BigDecimal baseQty;

    @Schema(description = "含损耗需求量", example = "105.00")
    private BigDecimal requiredQty;

    @Schema(description = "在库量快照", example = "20.00")
    private BigDecimal stockQty;

    @Schema(description = "实采量", example = "85.00")
    private BigDecimal purchaseQty;

    @Schema(description = "单价", example = "25.5000")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "2167.50")
    private BigDecimal totalAmount;

    @Schema(description = "历史价回填", example = "25.0000")
    private BigDecimal refUnitPrice;

    @Schema(description = "备注", example = "特急纱线")
    private String remark;

}

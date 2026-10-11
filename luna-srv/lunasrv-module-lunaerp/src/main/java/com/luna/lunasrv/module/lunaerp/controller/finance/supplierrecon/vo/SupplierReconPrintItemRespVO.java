package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应商对账单打印明细 Response VO（豁免脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconPrintItemRespVO {

    @Schema(description = "明细ID", example = "1")
    private Long id;

    @Schema(description = "行类型（PROCESS / PURCHASE / ADJUST）", example = "PROCESS")
    private String lineType;

    @Schema(description = "检验单号快照", example = "INS-001")
    private String inspectionOrderNo;

    @Schema(description = "检验类型快照（GREIGE / FINISHED）", example = "GREIGE")
    private String inspectionType;

    @Schema(description = "入库单号快照", example = "IN-001")
    private String stockInNo;

    @Schema(description = "关联纱线采购单号快照", example = "YO-001")
    private String yarnOrderNo;

    @Schema(description = "产品编号快照", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉布")
    private String productName;

    @Schema(description = "批次/缸号快照", example = "B001")
    private String batchNo;

    @Schema(description = "重量数量（KG）", example = "100.50")
    private BigDecimal qtyKg;

    @Schema(description = "米数数量（M）", example = "500.00")
    private BigDecimal qtyM;

    @Schema(description = "数量单位（KG/M）", example = "KG")
    private String qtyUnit;

    @Schema(description = "计价基准（KG / METER）", example = "METER")
    private String pricingBasis;

    @Schema(description = "结算计费数量", example = "500.00")
    private BigDecimal billQty;

    @Schema(description = "单价（元）", example = "2.5000")
    private BigDecimal unitPrice;

    @Schema(description = "行金额（元）", example = "1250.00")
    private BigDecimal lineAmount;

    @Schema(description = "调整原因（ADJUST行必填）", example = "劣质扣款")
    private String adjustReason;

    @Schema(description = "备注", example = "按实测米数结算")
    private String remark;

}

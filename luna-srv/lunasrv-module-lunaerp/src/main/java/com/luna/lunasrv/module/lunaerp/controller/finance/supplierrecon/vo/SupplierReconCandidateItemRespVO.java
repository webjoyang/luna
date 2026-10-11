package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 供应商对账候选明细 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconCandidateItemRespVO {

    @Schema(description = "行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROCESS")
    private String lineType;

    @Schema(description = "源单据类型（INSPECTION / STOCK_IN_ITEM）", example = "INSPECTION")
    private String sourceRefType;

    @Schema(description = "源单据主表ID（检验单ID / 入库单ID）", example = "1001")
    private Long sourceRefId;

    @Schema(description = "源单据明细ID（入库明细ID，检验单为汇总行时为空）", example = "2001")
    private Long sourceRefItemId;

    @Schema(description = "检验单ID快照（PROCESS 行）", example = "1001")
    private Long inspectionOrderId;

    @Schema(description = "检验单号快照（PROCESS 行）", example = "INS-20261011-001")
    private String inspectionOrderNo;

    @Schema(description = "检验类型快照（GREIGE 胚布检验 / FINISHED 成品检验）", example = "GREIGE")
    private String inspectionType;

    @Schema(description = "检验单已录卷数（PROCESS 行）", example = "12")
    private Integer rollCount;

    @Schema(description = "入库单ID快照（PURCHASE 行）", example = "3001")
    private Long stockInId;

    @Schema(description = "入库单号快照（PURCHASE 行）", example = "IN-20261011-001")
    private String stockInNo;

    @Schema(description = "关联纱线采购单ID快照（PURCHASE 行，可选）", example = "4001")
    private Long yarnOrderId;

    @Schema(description = "关联纱线采购单号快照（PURCHASE 行，可选）", example = "YO-20261011-001")
    private String yarnOrderNo;

    @Schema(description = "产品ID快照", example = "5001")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉胚布")
    private String productName;

    @Schema(description = "批次/缸号快照（PURCHASE 行）", example = "B20261001")
    private String batchNo;

    @Schema(description = "重量数量（KG，PROCESS：实称总重；PURCHASE：KG 单位量）", example = "100.50")
    private BigDecimal qtyKg;

    @Schema(description = "米数数量（M，PROCESS：实测总米数；PURCHASE：M 单位量）", example = "500.00")
    private BigDecimal qtyM;

    @Schema(description = "计价基准（KG / METER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "METER")
    private String pricingBasis;

    @Schema(description = "结算计费数量（基准对应量）", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal billQty;

    @Schema(description = "原始数量（PURCHASE 行）", example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "原始单位（PURCHASE 行，KG/M）", example = "M")
    private String unit;

    @Schema(description = "结算单价（元/KG 或 元/M）", example = "2.5000")
    private BigDecimal unitPrice;

    @Schema(description = "行金额（元，bill_qty * unit_price）", example = "1250.00")
    private BigDecimal lineAmount;

    @Schema(description = "源单据日期", example = "2026-10-10")
    private LocalDate orderDate;

}

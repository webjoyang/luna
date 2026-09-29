package com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生产管理 - 生产进度分页列表项 Response VO（只读聚合，ADR-0034）
 *
 * <p>扁平结构：合同头 + 各环节关键量 + 预警。所有 KG 量只聚合 KG 单位、排除 CANCELLED；
 * vatCoverage 为排产覆盖率百分比（0-100，2 位小数）。</p>
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理-生产进度分页列表项 Response VO")
@Data
public class ProductionProgressPageItemRespVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long contractId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CH20260001")
    private String contractNo;

    @Schema(description = "客户名称快照", example = "XX纺织")
    private String customerName;

    @Schema(description = "业务员姓名", example = "张三")
    private String salespersonName;

    @Schema(description = "交货日期（可空，无交期排最后）", example = "2026-09-30")
    private LocalDate deliveryDate;

    @Schema(description = "合同状态（ACTIVE/COMPLETED/CANCELLED）", example = "ACTIVE")
    private String status;

    @Schema(description = "合同单位（KG/M）", example = "KG")
    private String unit;

    @Schema(description = "合同需求量 KG（M 单位合同为 0）", example = "1000.00")
    private BigDecimal demandKg;

    @Schema(description = "胚布计划投胚量 KG 合计", example = "900.00")
    private BigDecimal greigePlannedKg;

    @Schema(description = "染色投染量 KG 合计", example = "800.00")
    private BigDecimal dyeingPlannedKg;

    @Schema(description = "已发染厂量 KG（发染记录聚合）", example = "700.00")
    private BigDecimal sentToDyeingKg;

    @Schema(description = "排产覆盖率（百分比 0-100，2 位小数）", example = "85.00")
    private BigDecimal vatCoverage;

    @Schema(description = "胚布入库量 KG", example = "600.00")
    private BigDecimal greigeInKg;

    @Schema(description = "成品入库量 KG", example = "500.00")
    private BigDecimal finishedInKg;

    @Schema(description = "已发货量 KG", example = "400.00")
    private BigDecimal shippedKg;

    @Schema(description = "合同未发量 KG（demandKg - shippedKg）", example = "600.00")
    private BigDecimal unshippedKg;

    @Schema(description = "发货单数", example = "3")
    private Integer shipmentCount;

    @Schema(description = "预警数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer alertCount;

    @Schema(description = "预警标签（临期/超期/延期缸）")
    private List<String> alerts;

}

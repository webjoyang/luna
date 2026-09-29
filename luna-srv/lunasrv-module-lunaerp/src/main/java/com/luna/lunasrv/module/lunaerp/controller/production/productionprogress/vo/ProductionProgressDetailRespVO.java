package com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo;

import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 生产管理 - 生产进度单合同全链路 Response VO（只读聚合，ADR-0034）
 *
 * <p>合同头 + 六环节分区（胚布/染色/检验/入库/出货）+ 预警 + 发染记录下钻。
 * 所有 KG 量只聚合 KG 单位、排除 CANCELLED。染色环节数据来自进度反馈覆盖层（ADR-0031）。</p>
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理-生产进度单合同全链路 Response VO")
@Data
public class ProductionProgressDetailRespVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long contractId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CH20260001")
    private String contractNo;

    @Schema(description = "客户名称快照", example = "XX纺织")
    private String customerName;

    @Schema(description = "业务员姓名", example = "张三")
    private String salespersonName;

    @Schema(description = "交货日期", example = "2026-09-30")
    private LocalDate deliveryDate;

    @Schema(description = "合同状态（ACTIVE/COMPLETED/CANCELLED）", example = "ACTIVE")
    private String status;

    @Schema(description = "合同单位（KG/M）", example = "KG")
    private String unit;

    @Schema(description = "胚布环节")
    private GreigeSection greige;

    @Schema(description = "染色环节")
    private DyeingSection dyeing;

    @Schema(description = "检验环节")
    private InspectionSection inspection;

    @Schema(description = "入库环节")
    private StockSection stock;

    @Schema(description = "出货环节")
    private ShipmentSection shipment;

    @Schema(description = "预警标签（临期/超期/延期缸）")
    private List<String> alerts;

    @Schema(description = "发染记录下钻明细")
    private List<GreigeDispatchRecordRespVO> greigeDispatchRecords;

    @Schema(description = "胚布环节进度")
    @Data
    public static class GreigeSection {

        @Schema(description = "合同需求量 KG", example = "1000.00")
        private BigDecimal demandKg;

        @Schema(description = "胚布计划投胚量 KG 合计", example = "900.00")
        private BigDecimal plannedKg;

        @Schema(description = "胚布入库量 KG（= stock.greigeInKg，同源）", example = "600.00")
        private BigDecimal stockedInKg;

        @Schema(description = "已发染厂量 KG", example = "700.00")
        private BigDecimal sentToDyeingKg;

        @Schema(description = "是否存在自织（SELF）胚布单", example = "true")
        private Boolean selfWeaving;

        @Schema(description = "自织计划匹数合计", example = "100")
        private Integer weavingPlannedPieces;

        @Schema(description = "自织完工匹数合计（织造单 COMPLETED）", example = "80")
        private Integer weavingCompletedPieces;
    }

    @Schema(description = "染色环节进度")
    @Data
    public static class DyeingSection {

        @Schema(description = "合同需求量 KG", example = "1000.00")
        private BigDecimal demandKg;

        @Schema(description = "染色投染量 KG 合计", example = "800.00")
        private BigDecimal plannedKg;

        @Schema(description = "排产覆盖率（百分比 0-100，2 位小数）", example = "85.00")
        private BigDecimal vatCoverage;

        @Schema(description = "缸状态汇总（状态 → 缸数）")
        private Map<String, Integer> vatStatusSummary;

        @Schema(description = "延期缸数", example = "1")
        private Integer delayedVatCount;
    }

    @Schema(description = "检验环节进度")
    @Data
    public static class InspectionSection {

        @Schema(description = "录检明细总数", example = "50")
        private Integer totalItems;

        @Schema(description = "等级汇总（A/B/C/DEFECTIVE → 条数）")
        private Map<String, Integer> gradeSummary;
    }

    @Schema(description = "入库环节进度")
    @Data
    public static class StockSection {

        @Schema(description = "胚布入库量 KG（greigeOrderId 非空）", example = "600.00")
        private BigDecimal greigeInKg;

        @Schema(description = "成品入库量 KG（dyeingOrderId 非空）", example = "500.00")
        private BigDecimal finishedInKg;
    }

    @Schema(description = "出货环节进度")
    @Data
    public static class ShipmentSection {

        @Schema(description = "已发货量 KG", example = "400.00")
        private BigDecimal shippedKg;

        @Schema(description = "合同未发量 KG（demandKg - shippedKg）", example = "600.00")
        private BigDecimal unshippedKg;

        @Schema(description = "发货单数", example = "3")
        private Integer shipmentCount;
    }

}

package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 供应商对账单打印数据 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconPrintRespVO {

    @Schema(description = "业务类型（固定 VENDOR_RECON，契约对齐 04 号通用件）", example = "VENDOR_RECON")
    private String bizType;

    @Schema(description = "打印标题", example = "供应商对账单")
    private String title;

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "SR20261015001")
    private String reconNo;

    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称快照", example = "杭州某某纺织厂")
    private String supplierName;

    @Schema(description = "供应商联系人", example = "李厂长")
    private String supplierContact;

    @Schema(description = "供应商联系电话", example = "13912345678")
    private String supplierPhone;

    @Schema(description = "对账区间起始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "状态（DRAFT / CONFIRMED / CANCELLED）", example = "CONFIRMED")
    private String status;

    @Schema(description = "对账总金额（元，打印豁免脱敏）", example = "15000.00")
    private BigDecimal totalAmount;

    @Schema(description = "加工费合计（元，打印豁免脱敏）", example = "8000.00")
    private BigDecimal totalProcessAmount;

    @Schema(description = "采购货款合计（元，打印豁免脱敏）", example = "7500.00")
    private BigDecimal totalPurchaseAmount;

    @Schema(description = "调整项合计（元，打印豁免脱敏）", example = "-500.00")
    private BigDecimal totalAdjustAmount;

    @Schema(description = "确认人ID", example = "1")
    private Long confirmedBy;

    @Schema(description = "确认人姓名", example = "王财务")
    private String confirmedByName;

    @Schema(description = "确认时间", example = "2026-10-15 12:00:00")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废人ID", example = "1")
    private Long cancelledBy;

    @Schema(description = "作废人姓名", example = "赵主管")
    private String cancelledByName;

    @Schema(description = "作废时间", example = "2026-10-15 14:00:00")
    private LocalDateTime cancelledAt;

    @Schema(description = "备注", example = "9月份结算对账")
    private String remark;

    @Schema(description = "制单人 / 创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-10-15 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "对账明细行列表（含加工/采购/调整三类行）")
    private List<SupplierReconPrintItemRespVO> items;

}

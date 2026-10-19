package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货单独立打印 Response VO（豁免脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnPrintRespVO {

    @Schema(description = "退货单ID", example = "10")
    private Long id;

    @Schema(description = "退货单号（RT-YYYYMMDD-NNN）", example = "RT-20261018-001")
    private String returnNo;

    @Schema(description = "业务状态（字典 lunaerp_sales_return_status：DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）", example = "POSTED")
    private String status;

    @Schema(description = "审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "APPROVED")
    private String approvalStatus;

    @Schema(description = "关联发货单ID", example = "100")
    private Long shipmentId;

    @Schema(description = "发货单号快照", example = "SH-20261018-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "200")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "盛泽纺织")
    private String customerName;

    @Schema(description = "客户联系人姓名", example = "王经理")
    private String contactName;

    @Schema(description = "客户联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "客户公司地址", example = "浙江省绍兴市柯桥区XX路88号")
    private String customerAddress;

    @Schema(description = "销售合同ID", example = "300")
    private Long salesContractId;

    @Schema(description = "销售合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "退货仓库ID", example = "400")
    private Long warehouseId;

    @Schema(description = "退货仓库名称快照", example = "柯桥成品一号仓")
    private String warehouseName;

    @Schema(description = "币种（RMB/USD/EUR/HKD）", example = "RMB")
    private String currency;

    @Schema(description = "退货总金额（元，豁免脱敏）", example = "1250.00")
    private BigDecimal totalAmount;

    @Schema(description = "退货总匹数", example = "1")
    private Integer totalPieces;

    @Schema(description = "回指入库单ID", example = "500")
    private Long stockInId;

    @Schema(description = "回指入库单号（只读展示）", example = "RK-20261018-001")
    private String stockInNo;

    @Schema(description = "回指入库单状态（只读展示）", example = "COMPLETED")
    private String stockInStatus;

    @Schema(description = "到货质检确认人ID", example = "1")
    private Long arrivedBy;

    @Schema(description = "到货质检确认时间")
    private LocalDateTime arrivedAt;

    @Schema(description = "财务过账确认人ID", example = "1")
    private Long postedBy;

    @Schema(description = "财务过账确认时间")
    private LocalDateTime postedAt;

    @Schema(description = "作废人ID", example = "1")
    private Long cancelledBy;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "作废原因", example = "客户取消")
    private String cancelReason;

    @Schema(description = "退货单据日期", example = "2026-10-18")
    private LocalDate returnDate;

    @Schema(description = "总备注", example = "质量问题整单退回")
    private String remark;

    @Schema(description = "制单人 / 创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "退货明细列表")
    private List<ItemVO> items;

    @Schema(description = "退货单打印明细行 VO（豁免脱敏）")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVO {
        @Schema(description = "退货明细ID", example = "101")
        private Long id;

        @Schema(description = "所属退货单ID", example = "10")
        private Long returnId;

        @Schema(description = "发货明细ID", example = "201")
        private Long shipmentItemId;

        @Schema(description = "产品ID", example = "301")
        private Long productId;

        @Schema(description = "产品编码快照", example = "PRD-001")
        private String productCode;

        @Schema(description = "产品名称快照", example = "纯棉斜纹")
        private String productName;

        @Schema(description = "客户色号快照", example = "WH-01")
        private String customerColorNo;

        @Schema(description = "颜色名称快照", example = "本白")
        private String colorName;

        @Schema(description = "缸号快照", example = "LOT-202609-01")
        private String vatNo;

        @Schema(description = "退货计费数量", example = "50.00")
        private BigDecimal returnQty;

        @Schema(description = "匹数", example = "1")
        private Integer pieces;

        @Schema(description = "单位（KG/M）", example = "KG")
        private String unit;

        @Schema(description = "单价（元，豁免脱敏）", example = "25.0000")
        private BigDecimal unitPrice;

        @Schema(description = "行金额（元，豁免脱敏）", example = "1250.00")
        private BigDecimal amount;

        @Schema(description = "退货原因（字典 lunaerp_sales_return_reason）", example = "QUALITY")
        private String returnReason;

        @Schema(description = "可二次入库数量", example = "50.00")
        private BigDecimal restockQty;

        @Schema(description = "废品数量", example = "0.00")
        private BigDecimal scrapQty;

        @Schema(description = "明细备注", example = "色差退回")
        private String remark;

        @Schema(description = "退货卷明细列表")
        private List<RollVO> rolls;
    }

    @Schema(description = "退货单打印卷明细 VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollVO {
        @Schema(description = "卷明细ID", example = "1001")
        private Long id;

        @Schema(description = "所属退货单ID", example = "10")
        private Long returnId;

        @Schema(description = "所属退货明细ID", example = "101")
        private Long returnItemId;

        @Schema(description = "关联原发货卷ID", example = "2001")
        private Long shipmentRollId;

        @Schema(description = "卷号", example = "R001")
        private String rollNo;

        @Schema(description = "实称重量（KG）", example = "50.00")
        private BigDecimal weight;

        @Schema(description = "米数（M）", example = "120.00")
        private BigDecimal lengthM;

        @Schema(description = "质检结果（RESTOCK 可二次入库 / SCRAP 废品）", example = "RESTOCK")
        private String qcResult;

        @Schema(description = "质检处置说明", example = "合格可二次销售")
        private String qcRemark;

        @Schema(description = "入库库位ID", example = "10")
        private Long locationId;

        @Schema(description = "入库库位编码", example = "A-01-01")
        private String locationCode;

        @Schema(description = "卷备注", example = "外包装完整")
        private String remark;
    }
}

package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货主表 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnRespVO {

    @Schema(description = "退货单ID", example = "10")
    private Long id;

    @Schema(description = "退货单号（RT-YYYYMMDD-NNN）", example = "RT-20261018-001")
    private String returnNo;

    @Schema(description = "业务状态（DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "发货单ID", example = "100")
    private Long shipmentId;

    @Schema(description = "发货单号快照", example = "SH-20261018-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "200")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "盛泽纺织")
    private String customerName;

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

    @Schema(description = "退货总金额（元，服务端权威重算）", example = "1250.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "退货总匹数（服务端权威重算）", example = "1")
    private Integer totalPieces;

    @Schema(description = "回指入库单ID", example = "500")
    private Long stockInId;

    @Schema(description = "回指入库单号（只读展示）", example = "RK-20261018-001")
    private String stockInNo;

    @Schema(description = "回指入库单状态（只读展示）", example = "DRAFT")
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

    @Schema(description = "作废原因", example = "客户取消退货申请")
    private String cancelReason;

    @Schema(description = "退货单据日期", example = "2026-10-18")
    private LocalDate returnDate;

    @Schema(description = "总备注", example = "客户整车退回")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "退货明细行列表")
    private List<SalesReturnItemRespVO> items;

}

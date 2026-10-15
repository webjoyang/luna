package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 供应商对账单 Response VO")
@Data
public class SupplierReconRespVO {

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "SR20261015001")
    private String reconNo;

    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称快照", example = "杭州某某纺织厂")
    private String supplierName;

    @Schema(description = "对账区间起始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "状态（DRAFT / CONFIRMED / CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "对账总金额（元）", example = "15000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "已申请金额（元，只读实时聚合）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal appliedAmount;

    @Schema(description = "确认人ID", example = "1")
    private Long confirmedBy;

    @Schema(description = "确认时间", example = "2026-10-15 12:00:00")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废人ID", example = "1")
    private Long cancelledBy;

    @Schema(description = "作废时间", example = "2026-10-15 14:00:00")
    private LocalDateTime cancelledAt;

    @Schema(description = "备注", example = "9月份结算对账")
    private String remark;

    @Schema(description = "创建时间", example = "2026-10-15 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "对账明细行列表")
    private List<SupplierReconItemRespVO> items;

}

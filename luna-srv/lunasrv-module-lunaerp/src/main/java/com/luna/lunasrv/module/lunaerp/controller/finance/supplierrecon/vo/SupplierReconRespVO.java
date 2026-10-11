package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

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

@Schema(description = "管理后台 - 供应商对账单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconRespVO {

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "SR20261011001")
    private String reconNo;

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "浙江某某织造有限公司")
    private String supplierName;

    @Schema(description = "对账区间开始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间结束日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "状态（DRAFT / CONFIRMED / CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "确认金额合计（元）", example = "25000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "确认人ID", example = "1")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废人ID", example = "1")
    private Long cancelledBy;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "备注", example = "9月份结算")
    private String remark;

    @Schema(description = "已申请金额（元，实时聚合，只读）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal appliedAmount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "对账明细列表")
    private List<SupplierReconItemRespVO> items;

}

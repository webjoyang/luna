package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

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

@Schema(description = "报价单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRespVO {

    @Schema(description = "报价单ID", example = "100")
    private Long id;

    @Schema(description = "报价单号", example = "QT20260001")
    private String quotationNo;

    @Schema(description = "版本号", example = "1")
    private Integer versionNo;

    @Schema(description = "客户档案ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "杭州某某服装有限公司")
    private String clientName;

    @Schema(description = "客户联系人快照", example = "张经理")
    private String clientContact;

    @Schema(description = "联系电话快照", example = "13800138000")
    private String phone;

    @Schema(description = "归属业务员", example = "1")
    private Long salespersonUserId;

    @Schema(description = "归属部门ID", example = "10")
    private Long deptId;

    @Schema(description = "报价日期", example = "2026-10-10")
    private LocalDate quotationDate;

    @Schema(description = "有效期自", example = "2026-10-10")
    private LocalDate validFrom;

    @Schema(description = "有效期至（空表示长期有效）", example = "2026-11-10")
    private LocalDate validTo;

    @Schema(description = "报价币种", example = "RMB")
    private String currency;

    @Schema(description = "汇率", example = "1.000000")
    private BigDecimal exchangeRate;

    @Schema(description = "总数量", example = "1000.00")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额（报价币种）", example = "30000.0000")
    private BigDecimal totalAmount;

    @Schema(description = "总成本额（脱敏字段）", example = "25500.0000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalCostAmount;

    @Schema(description = "整单加权毛利率（脱敏字段）", example = "0.150000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal avgMarginRate;

    @Schema(description = "状态（DRAFT / CONFIRMED / SUPERSEDED / CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "派生是否已失效（已确认且已过有效期至）", example = "false")
    private Boolean expired;

    @Schema(description = "低毛利预警（整单毛利率低于阈值或存在预警行）", example = "false")
    private Boolean lowMarginWarning;

    @Schema(description = "备注", example = "大客户特惠报价")
    private String remarks;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "报价明细列表")
    private List<QuotationItemRespVO> items;

}

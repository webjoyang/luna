package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 客户对账单列表 Response VO")
@Data
public class CustomerStatementRespVO {

    @Schema(description = "对账单ID", example = "1001")
    private Long id;

    @Schema(description = "对账单号", example = "CS20260001")
    private String statementNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户公司名", example = "盛泽纺织实业有限公司")
    private String customerName;

    @Schema(description = "对账币种", example = "RMB")
    private String currency;

    @Schema(description = "对账区间起始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "期初欠款余额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal openingBalance;

    @Schema(description = "本期应收发生额（借方合计）", example = "12000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal periodDebit;

    @Schema(description = "本期收款核销额（贷方合计）", example = "8000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal periodCredit;

    @Schema(description = "期末应收余额（元）", example = "9000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal closingBalance;

    @Schema(description = "状态（DRAFT/SENT/CONFIRMED/REJECTED/VOID）", example = "DRAFT")
    private String status;

    @Schema(description = "确认方式（ONLINE_PORTAL/ON_BEHALF）", example = "ON_BEHALF")
    private String confirmType;

    @Schema(description = "确认人姓名", example = "张财务")
    private String confirmByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认方式说明（线下代客确认方式）", example = "电话确认")
    private String confirmMethod;

    @Schema(description = "异议/驳回原因")
    private String rejectReason;

    @Schema(description = "财务回复异议备注")
    private String replyRemark;

    @Schema(description = "备注说明")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

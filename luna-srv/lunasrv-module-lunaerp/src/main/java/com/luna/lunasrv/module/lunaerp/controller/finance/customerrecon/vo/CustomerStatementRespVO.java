package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 客户对账单响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementRespVO {

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "CS20260001")
    private String statementNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称", example = "杭州明辉纺织有限公司")
    private String customerName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "对账期间开始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账期间结束日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "期初余额", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "本期借方发生额（发货应收增加）", example = "50000.00")
    private BigDecimal periodDebit;

    @Schema(description = "本期贷方发生额（回款/冲减）", example = "30000.00")
    private BigDecimal periodCredit;

    @Schema(description = "期末余额", example = "30000.00")
    private BigDecimal closingBalance;

    @Schema(description = "状态（DRAFT/SENT/CONFIRMED/REJECTED/VOID）", example = "DRAFT")
    private String status;

    @Schema(description = "确认方式类型（ONLINE_PORTAL/ON_BEHALF）", example = "ONLINE_PORTAL")
    private String confirmType;

    @Schema(description = "确认人姓名", example = "张财务")
    private String confirmByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认IP")
    private String confirmIp;

    @Schema(description = "确认方式/凭证备注")
    private String confirmMethod;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "驳回复核备注")
    private String replyRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String creator;

}

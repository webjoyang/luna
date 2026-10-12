package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 付款单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentPageReqVO extends PageParam {

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "资金账户ID", example = "2001")
    private Long accountId;

    @Schema(description = "收付款方向（IN 收款 / OUT 付款）", example = "IN")
    private String direction;

    @Schema(description = "收款类型（RECEIPT 货款 / ADVANCE 预收）", example = "RECEIPT")
    private String receiptType;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "单据状态（PENDING/COMPLETED/REVERSED）", example = "COMPLETED")
    private String status;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "模糊关键字（单号/经手人/水单号）", example = "RC2026")
    private String keyword;

    @Schema(description = "收款日期起始", example = "2026-10-01")
    private java.time.LocalDate receiptDateStart;

    @Schema(description = "收款日期截止", example = "2026-10-31")
    private java.time.LocalDate receiptDateEnd;

    @Schema(description = "创建时间起始", example = "2026-10-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间截止", example = "2026-10-31 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;

}

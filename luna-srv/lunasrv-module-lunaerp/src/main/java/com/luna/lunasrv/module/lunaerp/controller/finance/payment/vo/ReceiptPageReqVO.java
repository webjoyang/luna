package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 收款单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReceiptPageReqVO extends PageParam {

    @Schema(description = "客户ID", example = "2001")
    private Long customerId;

    @Schema(description = "资金账户ID", example = "3001")
    private Long accountId;

    @Schema(description = "收款类型（RECEIPT / ADVANCE）", example = "RECEIPT")
    private String receiptType;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "收款开始日期", example = "2026-10-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiptDateStart;

    @Schema(description = "收款结束日期", example = "2026-10-31")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiptDateEnd;

    @Schema(description = "收款单号（精确/模糊）", example = "RC-20261016-001")
    private String paymentNo;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 收款单保存（草稿） Request VO")
@Data
public class ReceiptSaveReqVO {

    @Schema(description = "收款单号（留空自动按 RC-{yyyyMMdd}-{3位日流水} 生成，也可手工指定）", example = "RC-20261016-001")
    private String paymentNo;

    @Schema(description = "收款类型（RECEIPT 货款 / ADVANCE 预收）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RECEIPT")
    @NotBlank(message = "收款类型不能为空")
    private String receiptType;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @Schema(description = "收款币种（RMB/USD等，严格同币种核销）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RMB")
    @NotBlank(message = "收款币种不能为空")
    private String currency;

    @Schema(description = "本次实收金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额必须大于0")
    private BigDecimal payAmount;

    @Schema(description = "收款日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-16")
    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    @Schema(description = "银行回单号/交易流水号", example = "BK202610160001")
    private String bankRefNo;

    @Schema(description = "经手人姓名", example = "张会计")
    private String handlerName;

    @Schema(description = "备注", example = "首期销售回款")
    private String remark;

    @Schema(description = "拟核销分配数组")
    @Valid
    private List<ReceiptItemReqVO> items;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 客户收款单保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptSaveReqVO {

    @Schema(description = "收款单ID（更新草稿时必填）", example = "100")
    private Long id;

    @Schema(description = "收款单号（可选手动填，留空则自动生成 RC-yyyyMMdd-xxx）", example = "RC-20261012-001")
    private String paymentNo;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "收款单类型（RECEIPT 货款 / ADVANCE 预收）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RECEIPT")
    @NotBlank(message = "收款单类型不能为空")
    private String receiptType;

    @Schema(description = "币种（默认 CNY）", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNY")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "收款日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-12")
    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    @Schema(description = "资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @Schema(description = "实收资金金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "实收金额不能为空")
    @DecimalMin(value = "0.01", message = "实收金额必须大于0")
    private BigDecimal payAmount;

    @Schema(description = "预收抵扣金额（元，预收冲应收时使用，缺省0）", example = "0.00")
    private BigDecimal prepayUsedAmount;

    @Schema(description = "银行水单/回单号", example = "B20261012001")
    private String bankRefNo;

    @Schema(description = "经手人姓名", example = "张三")
    private String handlerName;

    @Schema(description = "备注说明", example = "首期货款回款")
    private String remark;

    @Schema(description = "拟核销明细分配行（RECEIPT货款核销时选填）")
    @Valid
    private List<ReceiptRefSaveVO> items;

}

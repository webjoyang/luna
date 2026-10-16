package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 收款单修改（仅 PENDING 草稿可改） Request VO")
@Data
public class ReceiptUpdateReqVO {

    @Schema(description = "收款单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收款单ID不能为空")
    private Long id;

    @Schema(description = "收款类型（RECEIPT 货款 / ADVANCE 预收）", example = "RECEIPT")
    private String receiptType;

    @Schema(description = "资金账户ID", example = "3001")
    private Long accountId;

    @Schema(description = "本次实收金额（元）", example = "5000.00")
    @DecimalMin(value = "0.01", message = "收款金额必须大于0")
    private BigDecimal payAmount;

    @Schema(description = "收款日期", example = "2026-10-16")
    private LocalDate receiptDate;

    @Schema(description = "银行回单号/交易流水号", example = "BK202610160001")
    private String bankRefNo;

    @Schema(description = "经手人姓名", example = "张会计")
    private String handlerName;

    @Schema(description = "备注", example = "修改回款备注")
    private String remark;

    @Schema(description = "拟核销分配数组")
    @Valid
    private List<ReceiptItemReqVO> items;

}

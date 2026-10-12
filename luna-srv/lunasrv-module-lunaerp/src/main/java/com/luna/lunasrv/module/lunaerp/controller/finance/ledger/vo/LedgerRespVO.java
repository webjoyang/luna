package com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 经营分录账 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerRespVO {

    @Schema(description = "分录ID", example = "1024")
    private Long id;

    @Schema(description = "业务日期", example = "2026-10-12")
    private LocalDate bizDate;

    @Schema(description = "往来主体类型（CUSTOMER/SUPPLIER）", example = "CUSTOMER")
    private String partyType;

    @Schema(description = "往来主体ID", example = "1001")
    private Long partyId;

    @Schema(description = "往来主体名称", example = "环球外贸集团")
    private String partyName;

    @Schema(description = "资金账户ID", example = "1")
    private Long accountId;

    @Schema(description = "资金账户名称", example = "美金基本户")
    private String accountName;

    @Schema(description = "分录类别", example = "AR_INC")
    private String category;

    @Schema(description = "借贷方向（DEBIT/CREDIT）", example = "DEBIT")
    private String direction;

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "即期汇率", example = "7.20000000")
    private BigDecimal exchangeRate;

    @Schema(description = "原币金额", example = "1000.00")
    private BigDecimal originalAmount;

    @Schema(description = "折合本位币金额", example = "7200.00")
    private BigDecimal baseAmount;

    @Schema(description = "业务源单类型", example = "SHIPMENT")
    private String refType;

    @Schema(description = "业务源单ID", example = "2001")
    private Long refId;

    @Schema(description = "业务源单明细行ID", example = "3001")
    private Long refItemId;

    @Schema(description = "红冲回指原分录ID", example = "501")
    private Long reversalOfId;

    @Schema(description = "摘要备注", example = "发货应收")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 客户维度经营分录账余额与汇率差异 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCustomerBalanceRespVO {

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称", example = "环球外贸集团")
    private String customerName;

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "借方发生额合计（原币）", example = "3000.00")
    private BigDecimal totalDebit;

    @Schema(description = "贷方发生额合计（原币）", example = "1000.00")
    private BigDecimal totalCredit;

    @Schema(description = "原币未核销应收余额（借方 - 贷方）", example = "2000.00")
    private BigDecimal balance;

    @Schema(description = "历史入账折算本位币累计余额（元）", example = "14400.00")
    private BigDecimal historicalBaseBalance;

    @Schema(description = "当前即期汇率", example = "7.25000000")
    private BigDecimal currentRate;

    @Schema(description = "按当前即期汇率重算本位币价值（元）", example = "14500.00")
    private BigDecimal currentBaseBalance;

    @Schema(description = "汇差/汇率差异列（重算本位币价值 - 历史本位币价值，正数为汇兑收益，负数为汇兑损失）", example = "100.00")
    private BigDecimal exchangeDiff;

}

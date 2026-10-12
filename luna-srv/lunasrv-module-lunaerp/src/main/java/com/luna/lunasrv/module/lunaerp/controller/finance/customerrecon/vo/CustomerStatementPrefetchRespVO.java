package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 客户对账单取数预览响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementPrefetchRespVO {

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

    @Schema(description = "期初余额（起始日前未核销 AR 余额）", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "候选借方发生额合计", example = "50000.00")
    private BigDecimal candidateDebit;

    @Schema(description = "候选贷方发生额合计", example = "20000.00")
    private BigDecimal candidateCredit;

    @Schema(description = "预计期末余额（期初 + 借 - 贷）", example = "40000.00")
    private BigDecimal estimatedClosingBalance;

    @Schema(description = "候选明细行列表")
    private List<CustomerStatementCandidateItemVO> items;

}

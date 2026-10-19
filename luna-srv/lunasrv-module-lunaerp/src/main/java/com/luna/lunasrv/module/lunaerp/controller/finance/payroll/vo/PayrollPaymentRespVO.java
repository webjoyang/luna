package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工资发放批次 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentRespVO {

    @Schema(description = "发放批次ID", example = "1")
    private Long id;

    @Schema(description = "发放流水号", example = "GF20261019001")
    private String paymentNo;

    @Schema(description = "工资单主表ID", example = "100")
    private Long payrollId;

    @Schema(description = "工资单号快照", example = "GZ202610001")
    private String payrollNo;

    @Schema(description = "工资期间快照", example = "2026-10")
    private String period;

    @Schema(description = "付款资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "付款资金账户名称快照", example = "基本户")
    private String accountName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "原币发放金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "汇率快照", example = "1.00000000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal exchangeRate;

    @Schema(description = "本位币金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal baseAmount;

    @Schema(description = "发放支付日期", example = "2026-10-19")
    private LocalDate payDate;

    @Schema(description = "备注说明", example = "首批发放")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

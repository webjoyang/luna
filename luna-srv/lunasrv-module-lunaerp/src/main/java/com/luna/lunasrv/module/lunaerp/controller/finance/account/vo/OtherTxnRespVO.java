package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 其他收支单 Response VO")
@Data
public class OtherTxnRespVO {

    @Schema(description = "其他收支单ID", example = "1001")
    private Long id;

    @Schema(description = "其他收支单号", example = "QT20261019001")
    private String txnNo;

    @Schema(description = "资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "资金账户名称快照", example = "建设银行基本户")
    private String accountName;

    @Schema(description = "收支方向（DEBIT 流入 / CREDIT 流出）", example = "DEBIT")
    private String direction;

    @Schema(description = "收支类别", example = "INTEREST_IN")
    private String category;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "原币金额（元）", example = "150.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "汇率", example = "1.00000000")
    private BigDecimal exchangeRate;

    @Schema(description = "本位币金额（元）", example = "150.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal baseAmount;

    @Schema(description = "业务日期", example = "2026-10-19")
    private LocalDate txnDate;

    @Schema(description = "备注说明", example = "存款利息收入")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 资金账户间转账 Response VO")
@Data
public class TransferRespVO {

    @Schema(description = "转账单ID", example = "1001")
    private Long id;

    @Schema(description = "转账单号", example = "ZZ20261019001")
    private String transferNo;

    @Schema(description = "转出资金账户ID", example = "1001")
    private Long fromAccountId;

    @Schema(description = "转出账户名称快照", example = "工商银行基本户")
    private String fromAccountName;

    @Schema(description = "转入资金账户ID", example = "1002")
    private Long toAccountId;

    @Schema(description = "转入账户名称快照", example = "出纳现金备用金")
    private String toAccountName;

    @Schema(description = "转账币种", example = "RMB")
    private String currency;

    @Schema(description = "转账金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "转账业务日期", example = "2026-10-19")
    private LocalDate transferDate;

    @Schema(description = "状态（COMPLETED 固定登记即生效）", example = "COMPLETED")
    private String status;

    @Schema(description = "备注说明", example = "银行提现备用金")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

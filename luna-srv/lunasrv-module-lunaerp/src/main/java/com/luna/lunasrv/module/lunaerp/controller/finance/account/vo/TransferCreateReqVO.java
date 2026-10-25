package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 资金账户间转账创建 Request VO")
@Data
public class TransferCreateReqVO {

    @Schema(description = "转出资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "转出账户不能为空")
    private Long fromAccountId;

    @Schema(description = "转入资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    @NotNull(message = "转入账户不能为空")
    private Long toAccountId;

    @Schema(description = "币种（默认使用转出账户币种，跨币种不支持）", example = "RMB")
    private String currency;

    @Schema(description = "转账金额（元，必须大于0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "转账业务日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-19")
    @NotNull(message = "转账日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate transferDate;

    @Schema(description = "备注说明", example = "银行基本户提现至备用金")
    private String remark;

}

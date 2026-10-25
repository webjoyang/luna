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

@Schema(description = "管理后台 - 其他收支单创建 Request VO")
@Data
public class OtherTxnCreateReqVO {

    @Schema(description = "资金账户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @Schema(description = "收支方向（DEBIT 流入 / CREDIT 流出）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEBIT")
    @NotBlank(message = "收支方向不能为空")
    private String direction;

    @Schema(description = "收支类别（字典 lunaerp_other_txn_category：INTEREST_IN/WASTE_IN/OTHER_IN/FEE_OUT/COST_OUT/OTHER_OUT）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "INTEREST_IN")
    @NotBlank(message = "收支类别不能为空")
    private String category;

    @Schema(description = "原币金额（元，必须大于 0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    @NotNull(message = "收支金额不能为空")
    @DecimalMin(value = "0.01", message = "收支金额必须大于 0")
    private BigDecimal originalAmount;

    @Schema(description = "收支业务日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-19")
    @NotNull(message = "收支业务日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate txnDate;

    @Schema(description = "备注说明", example = "季度存款结息")
    private String remark;

}

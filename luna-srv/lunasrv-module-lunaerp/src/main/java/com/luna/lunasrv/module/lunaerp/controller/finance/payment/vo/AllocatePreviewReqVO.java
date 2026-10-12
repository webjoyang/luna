package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 自动核销分配试算 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocatePreviewReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "币种", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNY")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "收款待分配金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额必须大于0")
    private BigDecimal payAmount;

    @Schema(description = "指定参与分配的分录ID列表（空则默认客户名下该币种全部未核销分录）", example = "[1001, 1002]")
    private List<Long> entryIds;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 草稿对账单追加分录请求 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementAddEntryReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "对账单ID不能为空")
    private Long statementId;

    @Schema(description = "经营分录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "501")
    @NotNull(message = "分录ID不能为空")
    private Long ledgerEntryId;

}

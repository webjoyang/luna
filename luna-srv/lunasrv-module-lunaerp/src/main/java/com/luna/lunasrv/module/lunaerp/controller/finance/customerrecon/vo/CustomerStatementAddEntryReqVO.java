package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单草稿手工追加分录 Request VO")
@Data
public class CustomerStatementAddEntryReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long statementId;

    @Schema(description = "经营分录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "分录ID不能为空")
    private Long ledgerEntryId;

    @Schema(description = "行备注说明")
    private String remark;

}

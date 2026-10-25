package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单异议回复 Request VO")
@Data
public class CustomerStatementRejectReplyReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "财务回复异议备注", example = "已核对发货单重新调整数量")
    private String replyRemark;

}

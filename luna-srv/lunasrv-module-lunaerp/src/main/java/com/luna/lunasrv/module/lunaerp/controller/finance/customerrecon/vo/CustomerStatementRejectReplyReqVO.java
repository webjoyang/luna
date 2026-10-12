package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户对账单异议回复处理 Request VO")
@Data
public class CustomerStatementRejectReplyReqVO {

    @Schema(description = "对账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "对账单编号不能为空")
    private Long id;

    @Schema(description = "财务回复处理备注", example = "已核对合同与出库记录，修正后重新提交")
    private String replyRemark;

}

package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 发票申请驳回 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRejectReqVO {

    @Schema(description = "发票申请ID", example = "1024")
    private Long id;

    @Schema(description = "驳回原因", example = "客户开票信息税号不符，请修改后重新提交")
    private String reason;

}

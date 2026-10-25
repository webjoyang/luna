package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 发票申请分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvoiceApplicationPageReqVO extends PageParam {

    @Schema(description = "发票申请单号", example = "IA20260001")
    private String invoiceNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户公司名称", example = "盛泽纺织")
    private String customerName;

    @Schema(description = "状态（DRAFT/SUBMITTED/ISSUED/REJECTED）", example = "DRAFT")
    private String status;

    @Schema(description = "发票种类", example = "DIGITAL_SPECIAL")
    private String invoiceKind;

    @Schema(description = "官方发票号码（20位）", example = "24322000000000000001")
    private String officialInvoiceNo;

}

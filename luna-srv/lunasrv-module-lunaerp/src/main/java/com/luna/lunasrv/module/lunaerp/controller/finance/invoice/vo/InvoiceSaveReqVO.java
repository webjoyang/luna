package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 发票申请保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSaveReqVO {

    @Schema(description = "发票申请ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "币种（默认随发货单）", example = "CNY")
    private String currency;

    @Schema(description = "税率（13/9/6/3/1/0/EXEMPT，空则纺织默认13、外贸默认0）", example = "13")
    private String taxRate;

    @Schema(description = "发票种类（ELEC_SPECIAL/ELEC_NORMAL/VAT_SPECIAL/VAT_NORMAL）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ELEC_SPECIAL")
    @NotBlank(message = "发票种类不能为空")
    private String invoiceKind;

    @Schema(description = "是否电子发票（1是 0否，默认1）", example = "1")
    private Integer electronic;

    @Schema(description = "开票抬头（可修改，默认带出客户抬头/公司名）", example = "浙江某某进出口有限公司")
    private String invoiceTitle;

    @Schema(description = "统一社会信用代码/税号（可修改，默认带出）", example = "91330100MA2XXXXXXX")
    private String taxNo;

    @Schema(description = "开票地址电话（可修改，默认带出）", example = "杭州市西湖区某某路1号 0571-88888888")
    private String address;

    @Schema(description = "开户银行及账号（可修改，默认带出）", example = "工行西湖支行 6222021000000000")
    private String bankInfo;

    @Schema(description = "备注", example = "财务加急申请")
    private String remark;

    @Schema(description = "发票申请明细行", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一张发货单并填写开票金额")
    @Valid
    private List<InvoiceItemSaveVO> items;

}

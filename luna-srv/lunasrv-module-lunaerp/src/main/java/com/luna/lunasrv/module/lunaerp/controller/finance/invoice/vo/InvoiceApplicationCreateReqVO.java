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

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 发票申请创建 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceApplicationCreateReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @Schema(description = "币种（同发货单币种，禁混选）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RMB")
    @NotBlank(message = "币种不能为空")
    private String currency;

    @Schema(description = "开票税率百分比（如 13.00, 9.00, 0.00）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13.00")
    @NotNull(message = "开票税率不能为空")
    private BigDecimal taxRate;

    @Schema(description = "发票种类（DIGITAL_SPECIAL/DIGITAL_NORMAL/VAT_SPECIAL/VAT_NORMAL）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DIGITAL_SPECIAL")
    @NotBlank(message = "发票种类不能为空")
    private String invoiceKind;

    @Schema(description = "是否电子发票（1是 0否纸质）", example = "1")
    private Integer electronic;

    @Schema(description = "开票抬头快照（为空时自动从客户档案带出）", example = "苏州盛泽纺织有限公司")
    private String invoiceTitle;

    @Schema(description = "纳税人识别号/统一社会信用代码（为空时自动从客户档案带出）", example = "91320500XXXXXXXXXX")
    private String taxNo;

    @Schema(description = "开票地址电话（为空时自动从客户档案带出）", example = "江苏省苏州市吴江区盛泽镇 0512-XXXXXXXX")
    private String address;

    @Schema(description = "开户行及账号（为空时自动从客户档案带出）", example = "中国建设银行吴江支行 3200XXXXXXXXXXXX")
    private String bankInfo;

    @Schema(description = "备注说明", example = "按合同第一阶段开票")
    private String remark;

    @Schema(description = "申请明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "申请明细行不能为空")
    @Valid
    private List<InvoiceApplicationItemSaveReqVO> items;

}

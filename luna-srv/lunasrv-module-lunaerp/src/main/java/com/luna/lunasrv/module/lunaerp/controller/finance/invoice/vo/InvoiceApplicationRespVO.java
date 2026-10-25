package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 发票申请 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceApplicationRespVO {

    @Schema(description = "发票申请ID", example = "10001")
    private Long id;

    @Schema(description = "发票申请单号", example = "IA20260001")
    private String invoiceNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户公司名称", example = "盛泽纺织")
    private String customerName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "开票税率百分比", example = "13.00")
    private BigDecimal taxRate;

    @Schema(description = "发票种类", example = "DIGITAL_SPECIAL")
    private String invoiceKind;

    @Schema(description = "是否电子发票（1是 0否纸质）", example = "1")
    private Integer electronic;

    @Schema(description = "开票抬头", example = "苏州盛泽纺织有限公司")
    private String invoiceTitle;

    @Schema(description = "纳税人识别号/统一社会信用代码", example = "91320500XXXXXXXXXX")
    private String taxNo;

    @Schema(description = "开票地址电话", example = "江苏省苏州市吴江区盛泽镇 0512-XXXXXXXX")
    private String address;

    @Schema(description = "开户行及账号", example = "中国建设银行吴江支行 3200XXXXXXXXXXXX")
    private String bankInfo;

    @Schema(description = "状态（DRAFT/SUBMITTED/ISSUED/REJECTED）", example = "DRAFT")
    private String status;

    @Schema(description = "官方发票号码（20位）", example = "24322000000000000001")
    private String officialInvoiceNo;

    @Schema(description = "发票代码", example = "3200201130")
    private String invoiceCode;

    @Schema(description = "开票日期", example = "2026-10-18")
    private LocalDate invoiceDate;

    @Schema(description = "发票电子附件URL", example = "https://oss.lunaerp.com/invoice/20261018/inv01.pdf")
    private String attachmentUrl;

    @Schema(description = "不含税金额合计（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmountExclTax;

    @Schema(description = "税额合计（元）", example = "130.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalTax;

    @Schema(description = "价税合计总金额（元）", example = "1130.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmountInclTax;

    @Schema(description = "开票登记人", example = "admin")
    private String issueBy;

    @Schema(description = "开票登记时间")
    private LocalDateTime issueTime;

    @Schema(description = "提交人", example = "admin")
    private String submitBy;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "备注说明", example = "第一批次开票")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

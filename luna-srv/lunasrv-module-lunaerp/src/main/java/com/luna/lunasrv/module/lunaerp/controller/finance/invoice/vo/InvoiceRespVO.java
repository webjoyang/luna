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
import java.util.List;

@Schema(description = "管理后台 - 发票申请 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRespVO {

    @Schema(description = "发票申请ID", example = "1024")
    private Long id;

    @Schema(description = "发票申请单号", example = "IA20260001")
    private String invoiceNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "浙江某某进出口有限公司")
    private String customerName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "税率（13/9/6/3/1/0/EXEMPT）", example = "13")
    private String taxRate;

    @Schema(description = "发票种类", example = "ELEC_SPECIAL")
    private String invoiceKind;

    @Schema(description = "是否电子发票（1是 0否）", example = "1")
    private Integer electronic;

    @Schema(description = "开票抬头快照", example = "浙江某某进出口有限公司")
    private String invoiceTitle;

    @Schema(description = "统一社会信用代码/税号快照", example = "91330100MA2XXXXXXX")
    private String taxNo;

    @Schema(description = "开票地址电话快照", example = "杭州市西湖区某某路1号 0571-88888888")
    private String address;

    @Schema(description = "开户银行及账号快照", example = "工行西湖支行 6222021000000000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.BANK_CARD)
    private String bankInfo;

    @Schema(description = "状态（DRAFT/SUBMITTED/ISSUED/REJECTED）", example = "SUBMITTED")
    private String status;

    @Schema(description = "正式发票号码（20位）", example = "24332000000012345678")
    private String officialInvoiceNo;

    @Schema(description = "发票代码", example = "3300201130")
    private String invoiceCode;

    @Schema(description = "开票日期", example = "2026-10-12")
    private LocalDate invoiceDate;

    @Schema(description = "发票附件URL", example = "https://oss.example.com/invoice/20261012.pdf")
    private String attachmentUrl;

    @Schema(description = "不含税金额合计（元）", example = "8849.56")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmountExclTax;

    @Schema(description = "税额合计（元）", example = "1150.44")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalTax;

    @Schema(description = "价税合计（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmountInclTax;

    @Schema(description = "开票登记人", example = "财务李四")
    private String issueBy;

    @Schema(description = "开票登记时间")
    private LocalDateTime issueTime;

    @Schema(description = "提交人", example = "业务张三")
    private String submitBy;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "驳回原因", example = "税号信息有误")
    private String rejectReason;

    @Schema(description = "备注说明", example = "紧急加急申请")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "发票明细行列表")
    private List<InvoiceItemRespVO> items;

}

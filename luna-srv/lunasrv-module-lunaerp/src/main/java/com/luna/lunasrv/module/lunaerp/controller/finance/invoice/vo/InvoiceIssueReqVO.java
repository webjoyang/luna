package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "管理后台 - 发票登记/开票回填 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIssueReqVO {

    @Schema(description = "发票申请ID", example = "1024")
    private Long id;

    @Schema(description = "正式发票号码（20位数字必填，租户内全量唯一含软删）", requiredMode = Schema.RequiredMode.REQUIRED, example = "24332000000012345678")
    @NotBlank(message = "登记发票必须填写发票号码")
    @Pattern(regexp = "^\\d{20}$", message = "发票号码必须为20位数字")
    private String officialInvoiceNo;

    @Schema(description = "发票代码（旧式纸票可填，数电票通常为空）", example = "3300201130")
    private String invoiceCode;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-12")
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @Schema(description = "发票附件/PDF/图片下载URL", example = "https://oss.example.com/invoice/20261012.pdf")
    private String attachmentUrl;

}

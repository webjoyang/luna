package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 发票登记开票 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceApplicationIssueReqVO {

    @Schema(description = "发票申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "发票申请ID不能为空")
    private Long id;

    @Schema(description = "官方发票号码（20位）", requiredMode = Schema.RequiredMode.REQUIRED, example = "24322000000000000001")
    @NotBlank(message = "官方发票号码不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{20}$", message = "官方发票号码必须为 20 位字符")
    private String officialInvoiceNo;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-18")
    @NotNull(message = "开票日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate invoiceDate;

    @Schema(description = "发票代码（传统纸票可选，数电票为空）", example = "3200201130")
    private String invoiceCode;

    @Schema(description = "发票电子附件URL", example = "https://oss.lunaerp.com/invoice/20261018/inv01.pdf")
    private String attachmentUrl;

}

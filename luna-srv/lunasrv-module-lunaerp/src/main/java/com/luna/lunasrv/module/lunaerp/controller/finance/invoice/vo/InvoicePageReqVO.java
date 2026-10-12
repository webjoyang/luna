package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 发票申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvoicePageReqVO extends PageParam {

    @Schema(description = "模糊关键字（申请单号/客户名称/发票号码/开票抬头）", example = "IA2026")
    private String keyword;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "状态（DRAFT/SUBMITTED/ISSUED/REJECTED）", example = "SUBMITTED")
    private String status;

    @Schema(description = "发票种类（ELEC_SPECIAL/ELEC_NORMAL/VAT_SPECIAL/VAT_NORMAL）", example = "ELEC_SPECIAL")
    private String invoiceKind;

    @Schema(description = "币种（CNY/USD 等）", example = "CNY")
    private String currency;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "开票日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] invoiceDate;

}

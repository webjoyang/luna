package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 报价单打印数据 Response VO（白诺结构，绝对不含任何成本/毛利字段）")
@Data
public class QuotationPrintRespVO {

    @Schema(description = "报价单ID", example = "1024")
    private Long id;

    @Schema(description = "报价单号", example = "QT20260001")
    private String quotationNo;

    @Schema(description = "版本号", example = "1")
    private Integer versionNo;

    @Schema(description = "显示编号（QT...-vN）", example = "QT20260001-v1")
    private String displayNo;

    @Schema(description = "客户ID", example = "2048")
    private Long customerId;

    @Schema(description = "客户名称", example = "示例面料商")
    private String clientName;

    @Schema(description = "客户联系人", example = "张三")
    private String clientContact;

    @Schema(description = "联系电话", example = "13800000000")
    private String phone;

    @Schema(description = "业务员用户ID", example = "1")
    private Long salespersonUserId;

    @Schema(description = "业务员名称/昵称", example = "李业务")
    private String salespersonName;

    @Schema(description = "报价日期", example = "2026-03-29")
    private LocalDate quotationDate;

    @Schema(description = "有效期自", example = "2026-03-29")
    private LocalDate validFrom;

    @Schema(description = "有效期至", example = "2026-04-29")
    private LocalDate validTo;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "汇率", example = "1.0000")
    private BigDecimal exchangeRate;

    @Schema(description = "总数量", example = "1000.00")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额", example = "15500.00")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "报价有效期30天")
    private String remarks;

    @Schema(description = "明细列表（无成本/毛利字段）")
    private List<QuotationPrintItemRespVO> items;

}

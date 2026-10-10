package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 报价单转销售合同预填明细 Response VO")
@Data
public class QuotationContractPrefillItemRespVO {

    @Schema(description = "产品编号", example = "P001")
    private String productCode;

    @Schema(description = "品名", example = "纯棉平布")
    private String productName;

    @Schema(description = "客户色号", example = "RED-001")
    private String customerColor;

    @Schema(description = "成分", example = "100%棉")
    private String composition;

    @Schema(description = "门幅", example = "150cm")
    private String width;

    @Schema(description = "克重", example = "200g/m2")
    private String weight;

    @Schema(description = "数量", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "报价单价（折后最终报价）", example = "15.50")
    private BigDecimal unitPrice;

    @Schema(description = "其他费用", example = "0.00")
    private BigDecimal otherFees;

    @Schema(description = "行金额（数量×单价）", example = "1550.00")
    private BigDecimal amount;

    @Schema(description = "行备注", example = "加急")
    private String remark;

}

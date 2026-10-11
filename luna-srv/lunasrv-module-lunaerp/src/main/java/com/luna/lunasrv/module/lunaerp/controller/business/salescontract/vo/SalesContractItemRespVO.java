package com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理-销售合同明细 Response VO")
@Data
public class SalesContractItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long contractId;

    @Schema(description = "行号（按 line_no 升序返回）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

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

    @Schema(description = "单价", example = "15.50")
    private BigDecimal unitPrice;

    @Schema(description = "其他费用", example = "0.00")
    private BigDecimal otherFees;

    @Schema(description = "行金额（服务端重算：数量×单价+其他费用）", example = "1550.00")
    private BigDecimal amount;

    @Schema(description = "行备注", example = "加急")
    private String remark;

    @Schema(description = "已下达生产数量", example = "50.00")
    private BigDecimal releasedQty;

}

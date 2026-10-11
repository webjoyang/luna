package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 销售合同下达生产合同行 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseContractLineVO {

    @Schema(description = "合同行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long contractLineId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "产品档案ID", example = "101")
    private Long productId;

    @Schema(description = "产品编号", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉汗布")
    private String productName;

    @Schema(description = "成分", example = "100%棉")
    private String composition;

    @Schema(description = "门幅", example = "180cm")
    private String width;

    @Schema(description = "克重", example = "180g")
    private String gsm;

    @Schema(description = "客户色号", example = "C101")
    private String customerColor;

    @Schema(description = "合同数量", example = "1000.00")
    private BigDecimal contractQty;

    @Schema(description = "已下达量", example = "400.00")
    private BigDecimal releasedQty;

    @Schema(description = "剩余可下达量", example = "600.00")
    private BigDecimal remainingQty;

    @Schema(description = "默认损耗率", example = "0")
    private BigDecimal defaultLossRate;

}

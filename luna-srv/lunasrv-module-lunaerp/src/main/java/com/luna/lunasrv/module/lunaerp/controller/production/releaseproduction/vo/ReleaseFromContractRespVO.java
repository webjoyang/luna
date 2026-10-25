package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产预填 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseFromContractRespVO {

    @Schema(description = "销售合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long contractId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CH20260001")
    private String contractNo;

    @Schema(description = "客户名称", example = "某某纺织")
    private String clientName;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "合同明细行列表")
    private List<Line> lines;

    @Schema(description = "该合同/客户已有打色单列表")
    private List<ExistingColorInstruction> existingColorInstructions;

    @Schema(description = "合同明细行预填数据")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        @Schema(description = "合同行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long contractLineId;

        @Schema(description = "行号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer lineNo;

        @Schema(description = "产品编号", example = "P001")
        private String productCode;

        @Schema(description = "产品名称", example = "纯棉汗布")
        private String productName;

        @Schema(description = "成分", example = "100%棉")
        private String composition;

        @Schema(description = "幅宽", example = "180cm")
        private String width;

        @Schema(description = "克重", example = "200g/m²")
        private String gsm;

        @Schema(description = "客户色号", example = "C101")
        private String customerColor;

        @Schema(description = "合同数量", example = "1000.00")
        private BigDecimal contractQty;

        @Schema(description = "已下达量", example = "400.00")
        private BigDecimal releasedQty;

        @Schema(description = "剩余可下达量（contractQty - releasedQty）", example = "600.00")
        private BigDecimal remainingQty;

        @Schema(description = "默认损耗率（缺省 0）", example = "0")
        private BigDecimal defaultLossRate;
    }

    @Schema(description = "已有打色单简要信息")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExistingColorInstruction {
        @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
        private Long id;

        @Schema(description = "打色单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "DS20261007001")
        private String instructionNo;

        @Schema(description = "单据状态", example = "CONFIRMED")
        private String status;

        @Schema(description = "产品编号", example = "P001")
        private String productCode;

        @Schema(description = "产品名称", example = "纯棉汗布")
        private String productName;
    }

}

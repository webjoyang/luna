package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Schema(description = "管理后台 - 下达生产 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseProductionReqVO {

    @Schema(description = "销售合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "销售合同不能为空")
    private Long contractId;

    @Schema(description = "承织坯布厂供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "456")
    @NotNull(message = "承织坯布厂不能为空")
    private Long greigeFactoryId;

    @Schema(description = "承染方供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "789")
    @NotNull(message = "承染方不能为空")
    private Long dyeingFactoryId;

    @Schema(description = "产品分组列表（同产品合并下达）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "下达分组不能为空")
    @Valid
    private List<Group> groups;

    @Schema(description = "产品分组")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group {
        @Schema(description = "产品ID（可空，后端依 productCode 自动补全）", example = "2001")
        private Long productId;

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

        @Schema(description = "打色单模式：EXISTING 选已有 / NEW 新建 PENDING / NONE 不关联", example = "EXISTING")
        private String colorInstructionMode;

        @Schema(description = "已有打色指示单ID（EXISTING 模式时必填，须 CONFIRMED/IN_PRODUCTION）", example = "5")
        private Long existingColorInstructionId;

        @Schema(description = "新建打色单配置（NEW 模式时填）")
        @Valid
        private NewColorInstruction newColorInstruction;

        @Schema(description = "下达明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "下达明细行不能为空")
        @Valid
        private List<Line> lines;

        /**
         * 计算该分组所有下达行的下达数量合计
         */
        public BigDecimal calculateTotalReleaseQty() {
            if (lines == null || lines.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return lines.stream()
                    .map(Line::getReleaseQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * 计算该分组所有下达行中的最大损耗率
         */
        public BigDecimal calculateMaxLossRate() {
            if (lines == null || lines.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return lines.stream()
                    .map(Line::getLossRate)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
    }

    @Schema(description = "新建打色单配置")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewColorInstruction {
        @Schema(description = "打色承接方供应商ID（空=本厂打色房）", example = "3001")
        private Long labFactoryId;

        @Schema(description = "打色颜色行配置")
        @Valid
        private List<ColorLine> colors;
    }

    @Schema(description = "新建打色单颜色行配置")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorLine {
        @Schema(description = "合同行ID", example = "1")
        private Long contractLineId;

        @Schema(description = "客户色号", example = "C101")
        private String customerColorNo;

        @Schema(description = "颜色名称", example = "藏青")
        private String colorName;

        @Schema(description = "主灯源（必填，字典 lunaerp_color_light_source）", example = "D65")
        private String lightPrimary;

        @Schema(description = "辅灯源", example = "TL84")
        private String lightSecondary;

        @Schema(description = "厂内色号", example = "F01")
        private String factoryColorNo;

        @Schema(description = "色卡参考", example = "Pantone 19-4052")
        private String colorCardRef;
    }

    @Schema(description = "下达明细行")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        @Schema(description = "合同行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "合同行ID不能为空")
        private Long contractLineId;

        @Schema(description = "客户色号", example = "C101")
        private String customerColor;

        @Schema(description = "本次下达数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "600.00")
        @NotNull(message = "本次下达数量不能为空")
        @DecimalMin(value = "0.01", message = "本次下达数量必须大于0")
        private BigDecimal releaseQty;

        @Schema(description = "损耗率（缺省 0）", example = "0.05")
        private BigDecimal lossRate;
    }

}

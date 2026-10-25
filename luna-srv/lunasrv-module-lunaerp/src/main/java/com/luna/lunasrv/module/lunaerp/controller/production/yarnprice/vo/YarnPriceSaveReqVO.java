package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "生产管理 - 纱价保存请求 VO")
@Data
public class YarnPriceSaveReqVO {

    @Schema(description = "纱价记录ID（修改时必填）", example = "100")
    private Long id;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "纱线规格不能为空")
    private Long yarnSpecId;

    @Schema(description = "价类（PURCHASE 采购参考价 / MARKET 市场参考价）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PURCHASE")
    @NotBlank(message = "价类不能为空")
    private String priceType;

    @Schema(description = "单价（元/KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "35.5000")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0001", message = "单价必须大于 0")
    private BigDecimal price;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-10")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "来源（HAND 手工 / EXCEL 导入 / PURCHASE_AGG 采购聚合）", example = "HAND")
    private String source;

    @Schema(description = "备注", example = "月初调价")
    private String remark;

}

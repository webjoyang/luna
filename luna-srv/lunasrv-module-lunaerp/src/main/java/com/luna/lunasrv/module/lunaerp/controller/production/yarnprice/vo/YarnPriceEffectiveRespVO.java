package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "生产管理 - 纱价取价结果 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YarnPriceEffectiveRespVO {

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "价类", example = "PURCHASE")
    private String priceType;

    @Schema(description = "单价（元/KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "35.5000")
    private BigDecimal price;

    @Schema(description = "取到价格的生效日期（若回退或缺价为 null）", example = "2026-10-10")
    private LocalDate effectiveDate;

    @Schema(description = "价格来源：PRICE_TABLE 纱价表 / SPEC_REF_FALLBACK 规格参考价回退 / MISSING 缺价", example = "PRICE_TABLE")
    private String priceSource;

    @Schema(description = "是否缺价（无纱价且无参考单价时为 true）", example = "false")
    private Boolean isMissing;

}

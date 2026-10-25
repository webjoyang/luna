package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "生产管理 - 纱价响应 VO")
@Data
public class YarnPriceRespVO {

    @Schema(description = "纱价记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long id;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long yarnTypeId;

    @Schema(description = "规格名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "32S 棉")
    private String specName;

    @Schema(description = "价类", requiredMode = Schema.RequiredMode.REQUIRED, example = "PURCHASE")
    private String priceType;

    @Schema(description = "单价（元/KG）", requiredMode = Schema.RequiredMode.REQUIRED, example = "35.5000")
    private BigDecimal price;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-10")
    private LocalDate effectiveDate;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "HAND")
    private String source;

    @Schema(description = "备注", example = "月初调价")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

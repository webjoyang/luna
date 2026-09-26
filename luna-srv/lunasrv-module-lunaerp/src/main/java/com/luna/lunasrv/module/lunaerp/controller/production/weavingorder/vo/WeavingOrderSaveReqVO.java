package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理 - 织造单新建/修改 Request VO")
@Data
public class WeavingOrderSaveReqVO {

    @Schema(description = "织造单ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "织造单号（留空自动生成，可手填，租户内全量唯一含软删行）", example = "ZZ-PB0919001-01")
    private String code;

    @Schema(description = "关联胚布单ID（必填，限 productionMode=SELF 且未取消）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联胚布单不能为空")
    private Long greigeOrderId;

    @Schema(description = "计划匹数（正整数，必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "计划匹数不能为空")
    @Min(value = 1, message = "计划匹数必须为正整数")
    private Integer plannedQty;

    @Schema(description = "计划总重量 KG", example = "2500.00")
    @Min(value = 0, message = "计划总重量不能为负数")
    private BigDecimal plannedWeight;

    @Schema(description = "成分（手填优先，为空时从胚布单/产品档案带入）", example = "100% 棉")
    private String composition;

    @Schema(description = "克重（手填优先，为空时从胚布单/产品档案带入）", example = "220")
    private String gsm;

    @Schema(description = "门幅（手填优先，为空时从胚布单/产品档案带入）", example = "165cm")
    private String width;

    @Schema(description = "备注", example = "首批测试生产")
    private String remark;

}

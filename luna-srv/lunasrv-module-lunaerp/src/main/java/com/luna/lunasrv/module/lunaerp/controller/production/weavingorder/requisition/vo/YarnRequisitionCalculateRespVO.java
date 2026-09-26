package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-领纱测算 Response VO")
@Data
public class YarnRequisitionCalculateRespVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long weavingOrderId;

    @Schema(description = "织造单号", example = "ZZ-PB0919001-01")
    private String weavingOrderCode;

    @Schema(description = "产品档案ID", example = "200")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PRD001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "棉氨汗布")
    private String productName;

    @Schema(description = "计划匹数", example = "10")
    private Integer plannedQty;

    @Schema(description = "计划总重量 KG", example = "250.00")
    private BigDecimal plannedWeight;

    @Schema(description = "单匹计划重量 KG（plannedWeight / plannedQty）", example = "25.0000")
    private BigDecimal piecePlannedWeight;

    @Schema(description = "测算提示信息（如无 BOM、缺重量提示）", example = "该产品尚未配置纱线配比BOM，无法自动测算，请手工填写")
    private String tip;

    @Schema(description = "测算出的用纱需求明细列表（合并同规格）")
    private List<YarnRequisitionCalculateItemRespVO> items;

}

package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理 - 织造单 Response VO")
@Data
public class WeavingOrderRespVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "织造单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ-PB0919001-01")
    private String code;

    @Schema(description = "关联胚布单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long greigeOrderId;

    @Schema(description = "胚布单号快照", example = "PB0919001")
    private String greigeOrderNo;

    @Schema(description = "销售合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "关联产品档案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "成分快照", example = "100% 棉")
    private String composition;

    @Schema(description = "克重快照", example = "220")
    private String gsm;

    @Schema(description = "门幅快照", example = "165cm")
    private String width;

    @Schema(description = "计划匹数（正整数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer plannedQty;

    @Schema(description = "计划总重量 KG", example = "2500.00")
    private BigDecimal plannedWeight;

    @Schema(description = "完工匹数（只读，布票状态推导聚合）", example = "0")
    private Integer completedQty;

    @Schema(description = "织造单状态（字典 lunaerp_weaving_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    private String status;

    @Schema(description = "超量软警告信息（超胚布单总匹数时返回）", example = "织造单计划匹数合计(120匹)超出胚布单总匹数(100匹)")
    private String warning;

    @Schema(description = "备注", example = "车间备注")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}

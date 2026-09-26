package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-领纱单 Response VO")
@Data
public class YarnRequisitionRespVO {

    @Schema(description = "领纱单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "领纱单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "YS-ZZPB091900101-01")
    private String code;

    @Schema(description = "关联织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long weavingOrderId;

    @Schema(description = "织造单号快照", example = "ZZ-PB0919001-01")
    private String weavingOrderCode;

    @Schema(description = "产品ID", example = "200")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PRD001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "棉氨汗布")
    private String productName;

    @Schema(description = "织造单计划匹数", example = "10")
    private Integer plannedQty;

    @Schema(description = "织造单计划重量 KG", example = "250.00")
    private BigDecimal plannedWeight;

    @Schema(description = "领纱单状态（DRAFT/ACTIVE/PARTIAL/DISPATCHED/CLOSED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    private String status;

    @Schema(description = "备注", example = "加急领纱")
    private String remark;

    @Schema(description = "明细行数", example = "3")
    private Integer itemCount;

    @Schema(description = "总需求量（KG）", example = "250.00")
    private BigDecimal totalRequiredQty;

    @Schema(description = "总已发量（KG）", example = "0.00")
    private BigDecimal totalDispatchedQty;

    @Schema(description = "领纱明细列表")
    private List<YarnRequisitionItemRespVO> items;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

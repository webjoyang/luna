package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-胚布单 Response VO")
@Data
public class GreigeOrderRespVO {

    @Schema(description = "胚布单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "胚布单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PB0919001")
    private String orderNo;

    @Schema(description = "销售合同ID", example = "2048")
    private Long salesContractId;

    @Schema(description = "合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "承织坯布厂供应商ID（外发必填，自织可空）", example = "3072")
    private Long greigeFactoryId;

    @Schema(description = "坯布厂公司名快照（外发必填，自织可空）", example = "海宁坯布厂")
    private String greigeFactoryName;

    @Schema(description = "指定染厂供应商ID", example = "3073")
    private Long dyeingFactoryId;

    @Schema(description = "染厂公司名快照", example = "嘉兴染整厂")
    private String dyeingFactoryName;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-19")
    private LocalDate orderDate;

    @Schema(description = "交货日期", example = "2026-10-19")
    private LocalDate deliveryDate;

    @Schema(description = "产品档案ID", example = "4096")
    private Long productId;

    @Schema(description = "产品编号", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    private String productName;

    @Schema(description = "成分", example = "95棉5氨")
    private String composition;

    @Schema(description = "幅宽", example = "160cm 有效")
    private String width;

    @Schema(description = "克重", example = "180g")
    private String gsm;

    @Schema(description = "缩水率", example = "≤5%")
    private String shrinkage;

    @Schema(description = "匹重 KG（缺省 25）", example = "25.00")
    private BigDecimal pieceWeight;

    @Schema(description = "匹数（前端按 ceil(投胚量÷匹重) 自动算，可手改）", example = "40")
    private Integer pieceCount;

    @Schema(description = "计划投胚量 KG", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal plannedQty;

    @Schema(description = "下单成品数量 KG", example = "950.00")
    private BigDecimal finishedQty;

    @Schema(description = "预设损耗（小数，0.05 = 5%）", example = "0.05")
    private BigDecimal suggestedLoss;

    @Schema(description = "加工费", example = "3000.00")
    private BigDecimal processingFee;

    @Schema(description = "所属胚布品种（档案 id 为字符串）", example = "gt-001")
    private String greigeTypeId;

    @Schema(description = "生产工艺备注")
    private String productionRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "胚布单状态（字典 lunaerp_greige_order_status）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "生产模式（字典 lunaerp_greige_production_mode：SELF 自织 / OUTSOURCE 外发）", requiredMode = Schema.RequiredMode.REQUIRED, example = "OUTSOURCE")
    private String productionMode;

    @Schema(description = "织造执行进度（自织单实时聚合其下有效织造单进度，不落库；外发单为 null）")
    private GreigeOrderWeavingProgressVO weavingProgress;

    @Schema(description = "织造计划总匹数（自织单实时聚合，不落库；外发单为 null）", example = "50")
    private Integer weavingPlannedPieces;

    @Schema(description = "织造已完工总匹数（自织单实时聚合，不落库；外发单为 null）", example = "30")
    private Integer weavingCompletedPieces;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "颜色投胚分配行列表（仅详情接口按 line_no 升序嵌套返回，分页列表不返回）")
    private List<GreigeOrderColorRespVO> colors;

}

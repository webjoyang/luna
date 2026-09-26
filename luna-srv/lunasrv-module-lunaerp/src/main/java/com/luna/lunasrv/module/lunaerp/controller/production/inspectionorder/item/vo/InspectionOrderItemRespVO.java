package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-检验单录检明细 Response VO")
@Data
public class InspectionOrderItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long inspectionOrderId;

    @Schema(description = "卷号", example = "R001")
    private String rollNo;

    @Schema(description = "匹号", example = "P001")
    private String pieceNo;

    @Schema(description = "关联织造布票ID", example = "3001")
    private Long weavingTicketId;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "V001")
    private String vatNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal measuredWeight;

    @Schema(description = "米数", example = "100.00")
    private BigDecimal lengthM;

    @Schema(description = "机台号", example = "M-01")
    private String machineNo;

    @Schema(description = "等级（字典 lunaerp_inspection_grade：A/B/C/DEFECTIVE）", example = "A")
    private String grade;

    @Schema(description = "颜色名称", example = "藏青")
    private String colorName;

    @Schema(description = "产品色号", example = "C-01")
    private String colorNo;

    @Schema(description = "款号", example = "S-2026")
    private String styleNo;

    @Schema(description = "产品编码快照", example = "P001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "针织面料")
    private String productName;

    @Schema(description = "匹数", example = "1")
    private Integer pieceCount;

    @Schema(description = "备注", example = "无疵点")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

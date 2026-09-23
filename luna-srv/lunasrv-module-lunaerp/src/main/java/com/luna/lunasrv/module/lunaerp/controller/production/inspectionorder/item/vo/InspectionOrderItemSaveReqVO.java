package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-检验单录检明细创建/修改 Request VO")
@Data
public class InspectionOrderItemSaveReqVO {

    @Schema(description = "明细ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "所属检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "所属检验单不能为空")
    private Long inspectionOrderId;

    @Schema(description = "卷号", requiredMode = Schema.RequiredMode.REQUIRED, example = "R001")
    @NotBlank(message = "卷号不能为空")
    private String rollNo;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "V001")
    @NotBlank(message = "缸号不能为空")
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

}

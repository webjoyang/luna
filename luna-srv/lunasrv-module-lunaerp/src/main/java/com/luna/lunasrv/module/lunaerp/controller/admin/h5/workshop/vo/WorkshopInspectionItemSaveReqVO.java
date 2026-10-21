package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车间移动 H5 - 录检行保存 Request VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 录检行保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopInspectionItemSaveReqVO {

    @Schema(description = "录检明细ID（改录时由前端带回，新建传空）", example = "1024")
    private Long id;

    @Schema(description = "所属检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "所属检验单不能为空")
    private Long inspectionOrderId;

    @Schema(description = "关联织造布票ID（胚布 PIECE 模式必填带回）", example = "3001")
    private Long weavingTicketId;

    @Schema(description = "匹号（胚布 PIECE 模式：缺省布票完整 code，可改）", example = "BT-MTZZPB09190010101-0001")
    private String pieceNo;

    @Schema(description = "卷号（成品 ROLL 模式用）", example = "R001")
    private String rollNo;

    @Schema(description = "缸号（胚布 PIECE 模式可空；成品 ROLL 必填）", example = "V001")
    private String vatNo;

    @Schema(description = "等级（字典 2232 / lunaerp_inspection_grade：A/B/C/DEFECTIVE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "A")
    @NotBlank(message = "检验等级不能为空")
    private String grade;

    @Schema(description = "实称重量 KG", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.50")
    @NotNull(message = "实称重量不能为空")
    @DecimalMin(value = "0.01", message = "实称重量必须大于 0")
    private BigDecimal measuredWeight;

    @Schema(description = "米数", example = "100.00")
    private BigDecimal lengthM;

    @Schema(description = "备注", example = "边疵 2 处")
    private String remark;

    @Schema(description = "疵点现场照片 URL 列表（≤6 张）")
    private List<String> photos;

    public void setRemarks(String remarks) {
        this.remark = remarks;
    }

    public static class WorkshopInspectionItemSaveReqVOBuilder {
        public WorkshopInspectionItemSaveReqVOBuilder remarks(String remarks) {
            this.remark = remarks;
            return this;
        }
    }

}

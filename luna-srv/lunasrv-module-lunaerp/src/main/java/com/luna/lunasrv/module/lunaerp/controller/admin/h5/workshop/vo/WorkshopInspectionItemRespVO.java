package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车间移动 H5 - 录检明细精简出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 录检明细精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopInspectionItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "501")
    private Long id;

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inspectionOrderId;

    @Schema(description = "卷号（ROLL 模式）", example = "R001")
    private String rollNo;

    @Schema(description = "匹号（PIECE 模式）", example = "BT-001")
    private String pieceNo;

    @Schema(description = "关联织造布票ID", example = "2001")
    private Long weavingTicketId;

    @Schema(description = "缸号", example = "GH20261021-01")
    private String vatNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal measuredWeight;

    @Schema(description = "米数", example = "100.00")
    private BigDecimal lengthM;

    @Schema(description = "机台号", example = "LM01")
    private String machineNo;

    @Schema(description = "等级（A/B/C/DEFECTIVE）", example = "A")
    private String grade;

    @Schema(description = "颜色名称", example = "藏青色")
    private String colorName;

    @Schema(description = "产品色号", example = "C-01")
    private String colorNo;

    @Schema(description = "款号", example = "STYLE-01")
    private String styleNo;

    @Schema(description = "产品编码快照", example = "P20261001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "匹数", example = "1")
    private Integer pieceCount;

    @Schema(description = "备注", example = "边缘微疵")
    private String remark;

    @Schema(description = "疵点照片 URL 列表（≤6 张）")
    private List<String> photos;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

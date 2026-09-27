package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 生产管理-质检确认手工登记 Request VO")
@Data
public class InspectionConfirmationManualCreateReqVO {

    @Schema(description = "卷号", requiredMode = Schema.RequiredMode.REQUIRED, example = "R001")
    private String rollNo;

    @Schema(description = "等级（复用字典 lunaerp_inspection_grade：A/B/C/DEFECTIVE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "B")
    private String grade;

    @Schema(description = "责任类型（字典 lunaerp_inspection_responsibility：EQUIPMENT/OPERATOR/WAIVED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "OPERATOR")
    private String responsibility;

    @Schema(description = "挡车工用户ID（responsibility=OPERATOR 时必填）", example = "1024")
    private Long workerUserId;

    @Schema(description = "缸号", example = "V001")
    private String vatNo;

    @Schema(description = "机台号", example = "M-01")
    private String machineNo;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    private String productName;

    @Schema(description = "颜色名称", example = "藏青")
    private String colorName;

    @Schema(description = "产品色号", example = "C-01")
    private String colorNo;

    @Schema(description = "款号", example = "S-2026")
    private String styleNo;

    @Schema(description = "实称重量 KG", example = "25.50")
    private BigDecimal measuredWeight;

    @Schema(description = "米数", example = "100.00")
    private BigDecimal lengthM;

    @Schema(description = "来源检验单ID", example = "2048")
    private Long inspectionOrderId;

    @Schema(description = "认定时间（缺省当前时间，允许过去时间补录）", example = "2026-09-27 12:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime confirmedAt;

    @Schema(description = "原因备注", example = "破洞超标")
    private String note;

}

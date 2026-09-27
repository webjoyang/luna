package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-质检确认 批量打印视图 Response VO")
@Data
public class InspectionConfirmationPrintViewRespVO {

    @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "认定链标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String chainKey;

    @Schema(description = "来源（INSPECTION 检验单引入 / MANUAL 手工登记）", requiredMode = Schema.RequiredMode.REQUIRED, example = "INSPECTION")
    private String source;

    @Schema(description = "状态（字典 lunaerp_inspection_confirmation_status：PENDING/CONFIRMED/REVOKED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING")
    private String status;

    @Schema(description = "检验单号快照", example = "INS-20260925-001")
    private String inspectionOrderCode;

    @Schema(description = "卷号快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "P0309")
    private String rollNo;

    @Schema(description = "缸号快照", example = "V12")
    private String vatNo;

    @Schema(description = "等级快照（复用字典 lunaerp_inspection_grade）", requiredMode = Schema.RequiredMode.REQUIRED, example = "B")
    private String grade;

    @Schema(description = "机台号快照", example = "M01")
    private String machineNo;

    @Schema(description = "产品编码快照", example = "P20260901")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "颜色名称快照", example = "黑色")
    private String colorName;

    @Schema(description = "实称重量 KG 快照", example = "25.50")
    private BigDecimal measuredWeight;

    @Schema(description = "米数快照", example = "100.00")
    private BigDecimal lengthM;

    @Schema(description = "责任类型（字典 lunaerp_inspection_responsibility：EQUIPMENT/OPERATOR/WAIVED）", example = "OPERATOR")
    private String responsibility;

    @Schema(description = "挡车工用户ID", example = "1024")
    private Long workerUserId;

    @Schema(description = "挡车工姓名快照", example = "李桂香")
    private String workerName;

    @Schema(description = "认定人姓名快照", example = "张领班")
    private String confirmedByName;

    @Schema(description = "认定时间", example = "2026-09-27 14:00:00")
    private LocalDateTime confirmedAt;

    @Schema(description = "原因备注", example = "断纱")
    private String note;

    @Schema(description = "创建时间/发现日")
    private LocalDateTime createTime;

}

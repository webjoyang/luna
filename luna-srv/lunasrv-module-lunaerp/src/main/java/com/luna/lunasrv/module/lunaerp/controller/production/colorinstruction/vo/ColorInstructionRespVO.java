package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-打色指示单 Response VO")
@Data
public class ColorInstructionRespVO {

    @Schema(description = "打色指示单ID", example = "100")
    private Long id;

    @Schema(description = "打色单号", example = "DS20261010001")
    private String instructionNo;

    @Schema(description = "客户档案ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "宁波博洋家纺有限公司")
    private String clientName;

    @Schema(description = "客户联系人", example = "张经理 13800138000")
    private String contactPerson;

    @Schema(description = "产品档案ID", example = "2001")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P2026001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉斜纹面料")
    private String productName;

    @Schema(description = "打色承接方供应商ID", example = "3001")
    private Long labFactoryId;

    @Schema(description = "打色承接方公司名快照", example = "盛泽打色房")
    private String labFactoryName;

    @Schema(description = "成分快照", example = "100%棉")
    private String composition;

    @Schema(description = "幅宽快照", example = "160cm")
    private String width;

    @Schema(description = "克重快照", example = "220g/m²")
    private String gsm;

    @Schema(description = "单据日期", example = "2026-10-10")
    private LocalDate instructionDate;

    @Schema(description = "要求完成日", example = "2026-10-20")
    private LocalDate deliveryDate;

    @Schema(description = "单据状态（PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）", example = "PENDING")
    private String status;

    @Schema(description = "确认类型（CUSTOMER/INTERNAL）", example = "CUSTOMER")
    private String confirmType;

    @Schema(description = "客户反馈意见")
    private String customerFeedback;

    @Schema(description = "确认人用户ID")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "颜色行摘要列表（色号 chips + 轮次数，用于列表展示，防 N+1 预聚合）")
    private List<ColorInstructionSummaryVO> colorSummary;

    @Schema(description = "颜色行明细列表（用于详情展示）")
    private List<ColorInstructionColorRespVO> colors;

}

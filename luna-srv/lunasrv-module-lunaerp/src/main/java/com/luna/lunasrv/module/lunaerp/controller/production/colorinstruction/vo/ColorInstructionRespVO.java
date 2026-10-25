package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 打色指示单 Response VO")
@Data
public class ColorInstructionRespVO {

    @Schema(description = "打色指示单ID", example = "1001")
    private Long id;

    @Schema(description = "打色单号", example = "DS20261007001")
    private String instructionNo;

    @Schema(description = "客户档案ID", example = "1024")
    private Long customerId;

    @Schema(description = "客户名快照", example = "优衣库")
    private String clientName;

    @Schema(description = "客户联系人", example = "张三 13800138000")
    private String contactPerson;

    @Schema(description = "产品档案ID", example = "2048")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "32S 双面布")
    private String productName;

    @Schema(description = "打色承接方供应商ID", example = "3096")
    private Long labFactoryId;

    @Schema(description = "承接方公司名快照", example = "恒泰染整")
    private String labFactoryName;

    @Schema(description = "成分", example = "100% 棉")
    private String composition;

    @Schema(description = "幅宽", example = "180cm")
    private String width;

    @Schema(description = "克重", example = "200g/m²")
    private String gsm;

    @Schema(description = "单据日期", example = "2026-10-07")
    private LocalDate instructionDate;

    @Schema(description = "要求完成日", example = "2026-10-15")
    private LocalDate deliveryDate;

    @Schema(description = "单据状态：PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID", example = "PENDING")
    private String status;

    @Schema(description = "确认类型：CUSTOMER/INTERNAL", example = "CUSTOMER")
    private String confirmType;

    @Schema(description = "客户反馈意见", example = "确认通过")
    private String customerFeedback;

    @Schema(description = "确认人用户ID", example = "1")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "打色指示单颜色行明细列表")
    private List<ColorInstructionColorRespVO> colors;

    @Schema(description = "颜色行摘要列表（色号 chips，含轮次数与状态）")
    private List<ColorSummaryVO> colorSummaries;

    @Schema(description = "颜色行摘要 VO")
    @Data
    public static class ColorSummaryVO {

        @Schema(description = "客户色号", example = "C01")
        private String customerColorNo;

        @Schema(description = "颜色名称", example = "藏青")
        private String colorName;

        @Schema(description = "厂内色号", example = "F-01")
        private String factoryColorNo;

        @Schema(description = "色值 #rrggbb", example = "#000080")
        private String colorHex;

        @Schema(description = "打色轮次", example = "1")
        private Integer round;

        @Schema(description = "行状态：PENDING/APPROVED/REJECTED", example = "PENDING")
        private String status;
    }

}

package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 打色指示单独立打印 Response VO（豁免脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionPrintRespVO {

    @Schema(description = "打色指示单ID", example = "1024")
    private Long id;

    @Schema(description = "打色单号", example = "DS20261014001")
    private String instructionNo;

    @Schema(description = "单据日期", example = "2026-10-14")
    private LocalDate instructionDate;

    @Schema(description = "客户ID", example = "100")
    private Long customerId;

    @Schema(description = "客户名快照", example = "某某服饰有限公司")
    private String clientName;

    @Schema(description = "客户联系人（打印豁免全量）", example = "张三 13800138000")
    private String contactPerson;

    @Schema(description = "产品ID", example = "200")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "人棉弹力汗布")
    private String productName;

    @Schema(description = "成分快照", example = "95%人棉 5%氨纶")
    private String composition;

    @Schema(description = "幅宽快照", example = "180cm")
    private String width;

    @Schema(description = "克重快照", example = "180gsm")
    private String gsm;

    @Schema(description = "打色承接方供应商ID（空=本厂打色房）", example = "300")
    private Long labFactoryId;

    @Schema(description = "打色承接方公司名快照（空=本厂打色房自做）", example = "绍兴XX印染打色房")
    private String labFactoryName;

    @Schema(description = "要求完成日", example = "2026-10-20")
    private LocalDate deliveryDate;

    @Schema(description = "单据状态（PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）", example = "CONFIRMED")
    private String status;

    @Schema(description = "确认类型（CUSTOMER/INTERNAL）", example = "CUSTOMER")
    private String confirmType;

    @Schema(description = "客户反馈意见", example = "A样确认，做大货")
    private String customerFeedback;

    @Schema(description = "确认人用户ID", example = "1")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废原因", example = "客户取消打色需求")
    private String voidReason;

    @Schema(description = "单据备注", example = "注意手感要求")
    private String remark;

    @Schema(description = "制单人 / 创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "全部轮次颜色行（按 lineNo 排序）")
    private List<ColorItemVO> colors;

    @Schema(description = "打色指示单打印颜色行 VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorItemVO {

        @Schema(description = "颜色行ID", example = "10")
        private Long id;

        @Schema(description = "打色指示单ID", example = "1024")
        private Long instructionId;

        @Schema(description = "行号", example = "1")
        private Integer lineNo;

        @Schema(description = "客户色号", example = "C-101")
        private String customerColorNo;

        @Schema(description = "颜色名称", example = "藏青")
        private String colorName;

        @Schema(description = "厂内色号", example = "F-808")
        private String factoryColorNo;

        @Schema(description = "色值 #rrggbb", example = "#1B2A4A")
        private String colorHex;

        @Schema(description = "主灯源", example = "D65")
        private String lightPrimary;

        @Schema(description = "辅灯源", example = "TL84")
        private String lightSecondary;

        @Schema(description = "色卡参考", example = "Pantone TPX 19-4052")
        private String colorCardRef;

        @Schema(description = "份数", example = "3")
        private Integer copies;

        @Schema(description = "打色轮次", example = "1")
        private Integer round;

        @Schema(description = "行状态（PENDING/APPROVED/REJECTED）", example = "APPROVED")
        private String status;

        @Schema(description = "驳回意见", example = "偏红，要求重新打样")
        private String rejectReason;

        @Schema(description = "行备注", example = "加急")
        private String remark;

        @Schema(description = "样布照片列表")
        private List<String> samplePhotos;
    }
}

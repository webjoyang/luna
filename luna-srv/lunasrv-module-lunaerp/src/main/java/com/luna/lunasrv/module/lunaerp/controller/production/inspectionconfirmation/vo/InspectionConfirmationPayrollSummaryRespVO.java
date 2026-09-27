package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-质检确认 工资扣款汇总 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionConfirmationPayrollSummaryRespVO {

    @Schema(description = "汇总日期（YYYY-MM-DD）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-25")
    private LocalDate date;

    @Schema(description = "按挡车工分组列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkerGroup> groups;

    @Schema(description = "挡车工扣款分组")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerGroup {

        @Schema(description = "挡车工用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long workerUserId;

        @Schema(description = "挡车工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李桂香")
        private String workerName;

        @Schema(description = "扣款卷数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        private Integer count;

        @Schema(description = "扣款明细记录", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<PayrollRecord> records;
    }

    @Schema(description = "扣款明细记录")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollRecord {

        @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8801")
        private Long id;

        @Schema(description = "卷号", requiredMode = Schema.RequiredMode.REQUIRED, example = "P0309")
        private String rollNo;

        @Schema(description = "缸号", example = "V12")
        private String vatNo;

        @Schema(description = "等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "C")
        private String grade;

        @Schema(description = "检验单号", example = "INS-20260925-001")
        private String inspectionOrderCode;

        @Schema(description = "原因备注", example = "断纱")
        private String note;


        @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "INSPECTION")
        private String source;

        @Schema(description = "认定时间", example = "2026-09-25 17:40:12")
        private LocalDateTime confirmedAt;
    }
}

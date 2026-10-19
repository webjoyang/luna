package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 工资单次品扣款预览 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDefectPreviewRespVO {

    @Schema(description = "工资期间（YYYY-MM格式）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10")
    private String period;

    @Schema(description = "按员工分组列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkerGroup> groups;

    @Schema(description = "员工次品扣款分组")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerGroup {

        @Schema(description = "员工用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long workerUserId;

        @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
        private String workerName;

        @Schema(description = "次品卷数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        private Integer count;

        @Schema(description = "次品明细记录列表", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<DefectRecord> records;

    }

    @Schema(description = "次品明细记录")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectRecord {

        @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8801")
        private Long confirmationId;

        @Schema(description = "质检确认单号快照", example = "INS-20261001-001")
        private String confirmationNo;

        @Schema(description = "次品布卷号", requiredMode = Schema.RequiredMode.REQUIRED, example = "P0309")
        private String rollNo;

        @Schema(description = "缸号", example = "V12")
        private String vatNo;

        @Schema(description = "等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "C")
        private String grade;

        @Schema(description = "质检认定时间", example = "2026-10-15 14:30:00")
        private LocalDateTime confirmedAt;

        @Schema(description = "员工用户ID", example = "1024")
        private Long workerUserId;

        @Schema(description = "员工姓名", example = "李四")
        private String workerName;

        public Long getId() {
            return confirmationId;
        }

    }

}

package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 样品管理-样品分析付款统计 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleAnalysisPaymentStatsRespVO {

    @Schema(description = "未付金额（元，所有未付记录月结单价之和）", requiredMode = Schema.RequiredMode.REQUIRED, example = "90.00")
    private BigDecimal unpaidAmount;

    @Schema(description = "已付金额（元，所有已付记录月结单价之和）", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal paidAmount;

    @Schema(description = "任务总数（条数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long totalCount;

}

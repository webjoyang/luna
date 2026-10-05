package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 样品管理-样品分析付款状态更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleAnalysisPaymentReqVO {

    @Schema(description = "样品分析ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1024, 1025]")
    @NotEmpty(message = "样品分析ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "付款状态：false 未付 / true 已付", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "付款状态不能为空")
    private Boolean isPaid;

}

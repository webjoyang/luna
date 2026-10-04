package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 样品管理-样品分析分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleAnalysisPageReqVO extends PageParam {

    @Schema(description = "关键词（模糊命中 sampleAnalysisNo / clientName / productName / clientStyleNo / composition）", example = "SA2026")
    private String keyword;

    @Schema(description = "付款状态：false 未付 / true 已付（为 null 表示全部）", example = "false")
    private Boolean isPaid;

}

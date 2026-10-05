package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 样品管理-样品分析绕纱行创建/更新 Request VO")
@Data
public class SampleAnalysisYarnArrangeSaveReqVO {

    @Schema(description = "绕纱行ID（更新且行已存在时传入，新增行不传）", example = "1024")
    private Long id;

    @Schema(description = "路号（自由文本，可空）", example = "1")
    private String code;

    @Schema(description = "绕纱表述（如 25.1cm/1 花宽，可空）", example = "25.1cm/1 花宽")
    private String draftText;

    @Schema(description = "对针（如 第一路对针，可空）", example = "第一路对针")
    private String borderStatus;

    @Schema(description = "描述（如 开幅线，避让 SQL 保留词 desc，可空）", example = "开幅线")
    private String description;

}

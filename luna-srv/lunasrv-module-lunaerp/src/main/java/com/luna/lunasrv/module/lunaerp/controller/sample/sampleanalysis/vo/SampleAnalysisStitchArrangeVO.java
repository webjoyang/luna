package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Schema(description = "样品分析 - 三角排列画板数据")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleAnalysisStitchArrangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "列数", example = "24")
    private Integer columns;

    @Schema(description = "行数（固定 4 行）", example = "4")
    private Integer rows;

    @Schema(description = "网格数据（4 行 × columns 列，符号只允许空串/v/-/+/=）")
    private List<List<String>> data;
}

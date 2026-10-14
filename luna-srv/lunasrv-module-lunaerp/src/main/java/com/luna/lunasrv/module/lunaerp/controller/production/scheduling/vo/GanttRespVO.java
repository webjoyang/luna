package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 排产甘特图 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanttRespVO {

    @Schema(description = "织机列表（含空行，排除 INACTIVE）")
    private List<GanttLoomVO> looms;

    @Schema(description = "任务条带列表")
    private List<GanttBarVO> bars;

}

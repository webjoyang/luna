package com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 甘特图排产数据 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanttRespVO {

    @Schema(description = "织机列表（含窗口内无任务空行）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<GanttLoomVO> looms;

    @Schema(description = "任务条带列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<GanttBarVO> bars;

}

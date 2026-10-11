package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理-进度反馈排缸日历 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressFeedbackCalendarRespVO {

    @Schema(description = "排缸明细列表")
    private List<ProgressFeedbackCalendarVatVO> vats;

}

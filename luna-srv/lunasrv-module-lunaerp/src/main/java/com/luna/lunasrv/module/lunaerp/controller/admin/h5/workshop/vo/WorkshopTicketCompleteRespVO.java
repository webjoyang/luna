package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车间移动 H5 - 布票完工 Response VO（幂等友好）
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 布票完工 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketCompleteRespVO {

    @Schema(description = "是否已完工（若原先已处于完工/检验/入库态，返回 true；若本次从 STARTED 成功完工，返回 false）", example = "false")
    private Boolean alreadyCompleted;

}

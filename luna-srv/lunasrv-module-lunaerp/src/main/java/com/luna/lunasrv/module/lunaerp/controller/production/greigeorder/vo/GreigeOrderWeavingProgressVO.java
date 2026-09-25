package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 胚布单自织进度 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreigeOrderWeavingProgressVO {

    @Schema(description = "计划总匹数（自织单实时聚合其下有效织造单计划匹数之和，不落库）", example = "50")
    private Integer plannedPieces;

    @Schema(description = "已完工总匹数（自织单实时聚合其下有效织造单已完工匹数之和，不落库）", example = "30")
    private Integer completedPieces;

}

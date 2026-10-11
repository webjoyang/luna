package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 生产管理 - 齐套检查试算 Request VO（ADR-0041）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理 - 齐套检查试算 Request VO")
@Data
public class MaterialCheckReqVO {

    @Schema(description = "织造单ID列表（为空时试算全部 DRAFT/ACTIVE 织造单）", example = "[1, 2]")
    private List<Long> weavingOrderIds;

    @Schema(description = "仓库ID列表（为空时全仓库合计）", example = "[10, 20]")
    private List<Long> warehouseIds;

    @Schema(description = "是否含在途（缺省 false；仅影响汇总徽标口径，缺口两列常驻计算）", example = "false")
    private Boolean includeInTransit;

}

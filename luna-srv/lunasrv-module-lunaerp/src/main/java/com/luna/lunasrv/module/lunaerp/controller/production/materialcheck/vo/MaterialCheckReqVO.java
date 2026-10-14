package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 齐套检查试算 Request VO")
@Data
public class MaterialCheckReqVO {

    @Schema(description = "织造单ID列表（为空或空集合时，试算当前租户全部 DRAFT/ACTIVE 织造单）", example = "[100, 101]")
    private List<Long> weavingOrderIds;

    @Schema(description = "仓库ID列表（为空或空集合时，统计全部仓库合计）", example = "[1, 2]")
    private List<Long> warehouseIds;

    @Schema(description = "汇总徽标是否含在途（缺省 false，仅影响顶部汇总徽标统计口径，表格两缺口列常驻计算）", example = "false")
    private Boolean includeInTransit;

}

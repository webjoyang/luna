package com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 库存管理-库位 Response VO")
@Data
public class WarehouseLocationRespVO {

    @Schema(description = "库位ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "所属仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "库位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A-01-02")
    private String code;

    @Schema(description = "库位名称", example = "A区1排2层")
    private String name;

    @Schema(description = "排序值", example = "1")
    private Integer sort;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

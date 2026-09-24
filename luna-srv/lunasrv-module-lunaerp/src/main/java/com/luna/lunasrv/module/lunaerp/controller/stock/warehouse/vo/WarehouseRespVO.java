package com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 库存管理-仓库 Response VO")
@Data
public class WarehouseRespVO {

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WH20260001")
    private String warehouseNo;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "成品一号仓")
    private String name;

    @Schema(description = "仓库地址", example = "浙江省绍兴市柯桥区仓库路1号")
    private String address;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

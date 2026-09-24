package com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 库存管理-库位创建/修改 Request VO")
@Data
public class WarehouseLocationSaveReqVO {

    @Schema(description = "库位ID（修改时必填）", example = "2048")
    private Long id;

    @Schema(description = "所属仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @Schema(description = "库位编码（同一仓库内唯一，如 A-01-02）", requiredMode = Schema.RequiredMode.REQUIRED, example = "A-01-02")
    @NotBlank(message = "库位编码不能为空")
    private String code;

    @Schema(description = "库位名称", example = "A区1排2层")
    private String name;

    @Schema(description = "排序值（缺省 0）", example = "1")
    private Integer sort;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

}

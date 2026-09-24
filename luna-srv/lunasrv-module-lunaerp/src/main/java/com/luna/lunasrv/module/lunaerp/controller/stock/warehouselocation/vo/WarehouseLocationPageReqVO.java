package com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 库存管理-库位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseLocationPageReqVO extends PageParam {

    @Schema(description = "所属仓库ID", example = "1024")
    private Long warehouseId;

    @Schema(description = "智能搜索词（命中 code/name 之一）", example = "A-01")
    private String search;

    @Schema(description = "状态：ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

}

package com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 库存管理-仓库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehousePageReqVO extends PageParam {

    @Schema(description = "智能搜索词（命中 warehouseNo/name/address 之一）", example = "1号仓")
    private String search;

    @Schema(description = "状态：ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

}

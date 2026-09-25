package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 库存管理 - 库存余额分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryBalancePageReqVO extends PageParam {

    @Schema(description = "产品关键词（编码或名称）", example = "棉纱")
    private String productKeyword;

    @Schema(description = "仓库ID", example = "1")
    private Long warehouseId;

}

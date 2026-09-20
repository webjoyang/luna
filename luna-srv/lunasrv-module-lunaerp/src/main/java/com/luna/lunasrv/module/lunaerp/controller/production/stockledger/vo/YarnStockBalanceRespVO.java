package com.luna.lunasrv.module.lunaerp.controller.production.stockledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 纱线库存余额 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YarnStockBalanceRespVO {

    @Schema(description = "规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal stockQty;

}

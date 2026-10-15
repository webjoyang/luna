package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 供应商对账单更新 Request VO")
@Data
public class SupplierReconUpdateReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "备注", example = "9月份对账单（补充运费调整）")
    private String remark;

    @Schema(description = "对账明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "对账明细行列表不能为空")
    @Valid
    private List<SupplierReconItemSaveVO> items;

    @Schema(description = "显式删除的明细行ID列表")
    private List<Long> deletedItemIds;

}

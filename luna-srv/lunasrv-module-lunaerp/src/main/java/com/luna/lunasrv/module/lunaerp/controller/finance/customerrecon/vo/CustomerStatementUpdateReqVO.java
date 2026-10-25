package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 客户对账单修改 Request VO")
@Data
public class CustomerStatementUpdateReqVO {

    @Schema(description = "对账单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "对账单ID不能为空")
    private Long id;

    @Schema(description = "备注说明", example = "修改对账单备注")
    private String remark;

    @Schema(description = "待删除的明细行ID集合（仅草稿态可删除）")
    private List<Long> deletedItemIds;

}

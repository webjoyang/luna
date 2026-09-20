package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线采购测算 Request VO")
@Data
public class YarnOrderCalculateReqVO {

    @Schema(description = "关联胚布单ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[101, 102]")
    @NotEmpty(message = "请至少选择一张胚布指示单")
    private List<Long> greigeOrderIds;

    @Schema(description = "损耗率百分比数字（5 = 5%）", example = "5.00")
    private BigDecimal lossRate;

}

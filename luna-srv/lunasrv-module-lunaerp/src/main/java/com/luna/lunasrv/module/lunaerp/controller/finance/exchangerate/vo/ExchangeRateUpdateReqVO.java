package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 汇率主档更新 Request VO（仅用于停用与维护备注）")
@Data
public class ExchangeRateUpdateReqVO {

    @Schema(description = "汇率记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "汇率记录ID不能为空")
    private Long id;

    @Schema(description = "状态（ENABLE 启用 / DISABLE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DISABLE")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "备注说明（可清空）", example = "汇率调整停用")
    private String remark;

}

package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "生产管理 - 打色指示单作废 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionVoidReqVO {

    @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色指示单ID不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户取消订单")
    @NotBlank(message = "作废原因不能为空")
    private String voidReason;

}

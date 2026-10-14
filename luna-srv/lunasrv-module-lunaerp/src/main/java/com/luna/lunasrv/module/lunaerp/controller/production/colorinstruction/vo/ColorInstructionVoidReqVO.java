package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "生产管理 - 作废打色指示单 Request VO")
@Data
public class ColorInstructionVoidReqVO {

    @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色指示单ID不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户取消打色需求")
    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因长度不能超过 500 个字符")
    private String voidReason;

}

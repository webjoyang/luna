package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "生产管理 - 确认打色指示单 Request VO")
@Data
public class ColorInstructionConfirmReqVO {

    @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色指示单ID不能为空")
    private Long id;

    @Schema(description = "确认类型：CUSTOMER 客户确认 / INTERNAL 内部确认", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER")
    @NotBlank(message = "确认类型不能为空")
    private String confirmType;

    @Schema(description = "客户反馈意见", example = "客户对色通过")
    private String customerFeedback;

}

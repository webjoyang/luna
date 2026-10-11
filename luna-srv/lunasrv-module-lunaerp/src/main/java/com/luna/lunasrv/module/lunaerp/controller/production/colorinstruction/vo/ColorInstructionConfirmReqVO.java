package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "生产管理 - 打色指示单确认 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionConfirmReqVO {

    @Schema(description = "打色指示单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色指示单ID不能为空")
    private Long id;

    @Schema(description = "确认类型（CUSTOMER/INTERNAL）", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER")
    @NotNull(message = "确认类型不能为空")
    private String confirmType;

    @Schema(description = "客户反馈意见", example = "打色准确，同意按此投产")
    private String customerFeedback;

}

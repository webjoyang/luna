package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "生产管理 - 通过打色颜色行 Request VO")
@Data
public class ColorInstructionColorApproveReqVO {

    @Schema(description = "打色颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色颜色行ID不能为空")
    private Long id;

}

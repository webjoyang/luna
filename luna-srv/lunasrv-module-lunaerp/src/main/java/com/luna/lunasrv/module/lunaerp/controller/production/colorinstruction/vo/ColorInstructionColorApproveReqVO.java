package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "生产管理 - 打色指示单颜色行通过 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionColorApproveReqVO {

    @Schema(description = "颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "颜色行ID不能为空")
    private Long id;

}

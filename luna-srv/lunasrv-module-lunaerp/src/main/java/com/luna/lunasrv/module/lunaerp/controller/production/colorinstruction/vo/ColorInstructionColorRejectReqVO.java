package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "生产管理 - 驳回重打打色颜色行 Request VO")
@Data
public class ColorInstructionColorRejectReqVO {

    @Schema(description = "打色颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "打色颜色行ID不能为空")
    private Long id;

    @Schema(description = "驳回/否决意见", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "颜色偏深，需调浅半度")
    @jakarta.validation.constraints.Size(max = 500, message = "驳回意见长度不能超过 500 个字符")
    private String rejectReason;

}

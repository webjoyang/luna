package com.luna.lunasrv.module.lunaerp.common.recycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 回收站恢复 Request VO")
@Data
public class RecycleRestoreReqVO {

    @Schema(description = "业务对象类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER")
    @NotBlank(message = "业务对象类型不能为空")
    private String bizType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "业务ID不能为空")
    private Long bizId;

}

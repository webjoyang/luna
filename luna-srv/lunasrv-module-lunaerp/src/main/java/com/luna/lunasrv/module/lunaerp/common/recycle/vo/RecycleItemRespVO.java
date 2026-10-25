package com.luna.lunasrv.module.lunaerp.common.recycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回收站项 Response VO")
@Data
public class RecycleItemRespVO {

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER")
    private String bizType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long bizId;

    @Schema(description = "显示名称/单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某织造有限公司")
    private String displayName;

    @Schema(description = "删除人", example = "admin")
    private String deletedBy;

    @Schema(description = "删除时间", example = "2026-10-09 10:00:00")
    private LocalDateTime deletedTime;

}

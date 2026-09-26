package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理 - IoT 设备绑定 Response VO")
@Data
public class DeviceBindingRespVO {

    @Schema(description = "绑定ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "厂商侧设备唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEV-2026-001")
    private String vendorDeviceId;

    @Schema(description = "本系统织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long loomId;

    @Schema(description = "本系统机台号", example = "LM20260001")
    private String loomNo;

    @Schema(description = "织机型号", example = "34″·28G")
    private String loomModel;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

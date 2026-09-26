package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理 - IoT 设备绑定创建/修改 Request VO")
@Data
public class DeviceBindingSaveReqVO {

    @Schema(description = "绑定ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "厂商侧设备唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEV-2026-001")
    @NotBlank(message = "厂商侧设备标识不能为空")
    private String vendorDeviceId;

    @Schema(description = "本系统织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "织机ID不能为空")
    private Long loomId;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE", example = "ACTIVE")
    private String status;

}

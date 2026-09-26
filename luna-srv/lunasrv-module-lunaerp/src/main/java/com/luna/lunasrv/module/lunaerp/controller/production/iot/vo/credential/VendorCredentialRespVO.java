package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理 - IoT 厂商凭证 Response VO")
@Data
public class VendorCredentialRespVO {

    @Schema(description = "凭证ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "厂商账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "vendor_admin")
    private String vendorAccount;

    @Schema(description = "设备范围说明", example = "车间 A 区大圆机")
    private String scope;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "是否已配置密码", example = "true")
    private Boolean hasPassword;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

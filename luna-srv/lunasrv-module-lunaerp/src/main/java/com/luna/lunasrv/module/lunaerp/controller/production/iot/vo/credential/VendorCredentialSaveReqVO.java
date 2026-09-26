package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理 - IoT 厂商凭证创建/修改 Request VO")
@Data
public class VendorCredentialSaveReqVO {

    @Schema(description = "凭证ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "厂商账号（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "vendor_admin")
    @NotBlank(message = "厂商账号不能为空")
    private String vendorAccount;

    @Schema(description = "厂商密码（创建时必填，修改时若留空则保留原密码；保存即加密存储，不回显明文）", example = "secret123")
    private String vendorPassword;

    @Schema(description = "设备范围说明", example = "车间 A 区大圆机")
    private String scope;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE", example = "ACTIVE")
    private String status;

}

package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理 - IoT 厂商凭证分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorCredentialPageReqVO extends PageParam {

    @Schema(description = "搜索关键词（账号 / 范围）", example = "admin")
    private String search;

    @Schema(description = "厂商账号", example = "vendor_admin")
    private String vendorAccount;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}

package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理 - IoT 设备绑定分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceBindingPageReqVO extends PageParam {

    @Schema(description = "搜索关键词（厂商设备标识）", example = "DEV-001")
    private String search;

    @Schema(description = "厂商侧设备标识", example = "DEV-001")
    private String vendorDeviceId;

    @Schema(description = "织机ID", example = "2001")
    private Long loomId;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}

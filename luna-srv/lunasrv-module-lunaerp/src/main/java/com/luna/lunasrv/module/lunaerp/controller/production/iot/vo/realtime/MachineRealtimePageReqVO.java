package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理 - IoT 实时快照分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineRealtimePageReqVO extends PageParam {

    @Schema(description = "搜索关键词（厂商设备标识 / 设备编号 / 在班人员）", example = "DEV-001")
    private String search;

    @Schema(description = "厂商侧设备标识", example = "DEV-001")
    private String vendorDeviceId;

    @Schema(description = "状态（running/idle/abnormal/disabled/offline）", example = "running")
    private String status;

}

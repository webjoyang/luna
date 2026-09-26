package com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理 - IoT 实时快照 Response VO")
@Data
public class MachineRealtimeRespVO {

    @Schema(description = "快照ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "厂商侧设备唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEV-2026-001")
    private String vendorDeviceId;

    @Schema(description = "设备出厂编号/SN", example = "SN-998822")
    private String deviceSn;

    @Schema(description = "关联织机ID", example = "2001")
    private Long loomId;

    @Schema(description = "关联机台号", example = "LM20260001")
    private String loomNo;

    @Schema(description = "实时状态（running/idle/abnormal/disabled/offline）", example = "running")
    private String status;

    @Schema(description = "运转效率百分比（%）", example = "92.50")
    private BigDecimal runEfficiencyPct;

    @Schema(description = "分钟圈速（RPM）", example = "26")
    private Integer minuteCircleSpeed;

    @Schema(description = "班次圈数", example = "12500")
    private Integer shiftCircles;

    @Schema(description = "限制圈速", example = "28")
    private Integer limitSpeed;

    @Schema(description = "运转分钟", example = "450")
    private Integer runMinutes;

    @Schema(description = "在班分钟", example = "480")
    private Integer shiftMinutes;

    @Schema(description = "在班人员", example = "张三")
    private String staffOnShift;

    @Schema(description = "采集/录入时间")
    private LocalDateTime fetchedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

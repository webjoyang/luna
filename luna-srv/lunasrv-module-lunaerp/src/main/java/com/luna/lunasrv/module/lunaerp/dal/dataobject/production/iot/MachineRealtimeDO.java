package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产管理 - IoT 设备实时快照 DO
 *
 * 表：lunaerp_production_machine_realtime
 * (tenant, vendorDeviceId) 唯一，只保留最新值（更新覆盖）
 * 快照为辅助列，不回写 loom.status（信息源不污染）
 *
 * @author luna
 */
@TableName("lunaerp_production_machine_realtime")
@KeySequence("lunaerp_production_machine_realtime_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineRealtimeDO extends TenantBaseDO {

    /**
     * 快照ID
     */
    private Long id;

    /**
     * 厂商侧设备唯一标识
     */
    private String vendorDeviceId;

    /**
     * 设备编号 / 出厂编号
     */
    private String deviceSn;

    /**
     * 实时状态（running / idle / abnormal / disabled / offline）
     */
    private String status;

    /**
     * 运转效率百分比（%）
     */
    private BigDecimal runEfficiencyPct;

    /**
     * 分钟圈速（RPM）
     */
    private Integer minuteCircleSpeed;

    /**
     * 班次圈数
     */
    private Integer shiftCircles;

    /**
     * 限制圈速
     */
    private Integer limitSpeed;

    /**
     * 运转分钟
     */
    private Integer runMinutes;

    /**
     * 在班分钟
     */
    private Integer shiftMinutes;

    /**
     * 在班人员
     */
    private String staffOnShift;

    /**
     * 采集 / 录入时间
     */
    private LocalDateTime fetchedAt;

}

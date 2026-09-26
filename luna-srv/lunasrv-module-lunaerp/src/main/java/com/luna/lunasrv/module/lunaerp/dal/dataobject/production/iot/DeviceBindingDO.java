package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 生产管理 - IoT 设备绑定 DO
 *
 * 表：lunaerp_production_device_binding
 * vendorDeviceId 厂商侧设备唯一标识
 * loomId 绑定本系统织机
 * status ACTIVE/INACTIVE
 * lastSyncAt 最后同步时间
 * 归属以绑定表为权威路由，同设备重复绑定报 411
 *
 * @author luna
 */
@TableName("lunaerp_production_device_binding")
@KeySequence("lunaerp_production_device_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceBindingDO extends TenantBaseDO {

    /**
     * 绑定ID
     */
    private Long id;

    /**
     * 厂商侧设备唯一标识
     */
    private String vendorDeviceId;

    /**
     * 本系统织机ID
     */
    private Long loomId;

    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE
     */
    private String status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncAt;

}

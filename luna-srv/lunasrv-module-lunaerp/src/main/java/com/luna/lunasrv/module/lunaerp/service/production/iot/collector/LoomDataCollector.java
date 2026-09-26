package com.luna.lunasrv.module.lunaerp.service.production.iot.collector;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.MachineRealtimeDO;

import java.util.List;

/**
 * 织机 IoT 数据采集器接口（ADR-0028）
 *
 * 抽象按凭证/绑定拉取设备清单与最新快照能力，输出统一的 MachineRealtimeDO。
 *
 * @author luna
 */
public interface LoomDataCollector {

    /**
     * 采集所有有效绑定的实时快照
     *
     * @return 实时快照列表
     */
    List<MachineRealtimeDO> collectAll();

    /**
     * 按厂商凭证拉取其范围内的设备实时快照（ADR-0028）
     *
     * @param credential 厂商凭证
     * @return 实时快照列表
     */
    List<MachineRealtimeDO> collectByCredential(com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.VendorCredentialDO credential);

    /**
     * 采集单个设备绑定的实时快照
     *
     * @param binding 设备绑定
     * @return 实时快照
     */
    MachineRealtimeDO collectByBinding(DeviceBindingDO binding);

}

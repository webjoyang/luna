package com.luna.lunasrv.module.lunaerp.service.production.iot.collector;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.MachineRealtimeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.DeviceBindingMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 织机数据采集模拟实现（ADR-0028）
 *
 * 按绑定关系生成稳定的模拟快照值，不启用真实定时任务，可供手工触发/单测验证。
 *
 * @author luna
 */
@Component
public class SimulatedLoomDataCollector implements LoomDataCollector {

    @Resource
    private DeviceBindingMapper deviceBindingMapper;

    @Override
    public List<MachineRealtimeDO> collectAll() {
        List<DeviceBindingDO> bindings = deviceBindingMapper.selectListByStatus("ACTIVE");
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<MachineRealtimeDO> list = new ArrayList<>();
        for (DeviceBindingDO binding : bindings) {
            list.add(collectByBinding(binding));
        }
        return list;
    }

    @Override
    public List<MachineRealtimeDO> collectByCredential(com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.VendorCredentialDO credential) {
        if (credential == null) {
            return Collections.emptyList();
        }
        return collectAll();
    }

    @Override
    public MachineRealtimeDO collectByBinding(DeviceBindingDO binding) {
        if (binding == null) {
            return null;
        }
        MachineRealtimeDO realtime = new MachineRealtimeDO();
        realtime.setVendorDeviceId(binding.getVendorDeviceId());
        realtime.setDeviceSn("SN-" + binding.getVendorDeviceId());

        long seed = binding.getId() != null ? binding.getId() : (binding.getVendorDeviceId() != null ? binding.getVendorDeviceId().hashCode() : 1);
        boolean isRunning = (seed % 5 != 0);
        realtime.setStatus(isRunning ? "running" : "idle");

        BigDecimal efficiency = isRunning
                ? BigDecimal.valueOf(90.0 + (Math.abs(seed) % 80) / 10.0).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        realtime.setRunEfficiencyPct(efficiency);
        realtime.setMinuteCircleSpeed(isRunning ? (int) (22 + (seed % 6)) : 0);
        realtime.setShiftCircles((int) (10000 + (seed % 3000)));
        realtime.setLimitSpeed(28);
        realtime.setRunMinutes(isRunning ? 430 : 200);
        realtime.setShiftMinutes(480);
        realtime.setStaffOnShift("班组A");
        realtime.setFetchedAt(LocalDateTime.now());
        return realtime;
    }

}

package com.luna.lunasrv.module.lunaerp.service.production.iot.realtime;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.MachineRealtimeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.DeviceBindingMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.MachineRealtimeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.service.production.iot.collector.LoomDataCollector;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产管理 - IoT 设备实时快照 Service 实现类
 *
 * 核心设计（ADR-0028）：
 * 1. 唯一性：(tenant_id, vendor_device_id) 唯一，只保留最新值（更新覆盖）；
 * 2. 信息源不互相污染：IoT 快照绝不回写 loom.status（织机状态由人工维护）；
 * 3. 权威路由：归属由 DeviceBinding 映射决定。
 *
 * @author luna
 */
@Service
@Validated
public class MachineRealtimeServiceImpl implements MachineRealtimeService {

    @Resource
    private MachineRealtimeMapper machineRealtimeMapper;

    @Resource
    private DeviceBindingMapper deviceBindingMapper;

    @Resource
    private LoomMapper loomMapper;

    @Resource
    private LoomDataCollector loomDataCollector;

    @Override
    public Long recordRealtimeData(MachineRealtimeSaveReqVO saveReqVO) {
        String vendorDeviceId = saveReqVO.getVendorDeviceId().trim();
        MachineRealtimeDO existing = machineRealtimeMapper.selectByVendorDeviceId(vendorDeviceId);

        LocalDateTime fetchTime = saveReqVO.getFetchedAt() != null ? saveReqVO.getFetchedAt() : LocalDateTime.now();
        if (existing != null) {
            // 更新覆盖最新快照
            existing.setDeviceSn(saveReqVO.getDeviceSn());
            if (StringUtils.hasText(saveReqVO.getStatus())) {
                existing.setStatus(saveReqVO.getStatus());
            }
            existing.setRunEfficiencyPct(saveReqVO.getRunEfficiencyPct());
            existing.setMinuteCircleSpeed(saveReqVO.getMinuteCircleSpeed());
            existing.setShiftCircles(saveReqVO.getShiftCircles());
            existing.setLimitSpeed(saveReqVO.getLimitSpeed());
            existing.setRunMinutes(saveReqVO.getRunMinutes());
            existing.setShiftMinutes(saveReqVO.getShiftMinutes());
            existing.setStaffOnShift(saveReqVO.getStaffOnShift());
            existing.setFetchedAt(fetchTime);
            machineRealtimeMapper.updateById(existing);
            return existing.getId();
        } else {
            // 首次录入插入
            MachineRealtimeDO newDO = BeanUtils.toBean(saveReqVO, MachineRealtimeDO.class);
            newDO.setVendorDeviceId(vendorDeviceId);
            if (!StringUtils.hasText(newDO.getStatus())) {
                newDO.setStatus("offline");
            }
            newDO.setFetchedAt(fetchTime);
            machineRealtimeMapper.insert(newDO);
            return newDO.getId();
        }
        // 注意：严格遵守 ADR-0028，绝不调用 loomMapper.updateById 或修改 loom.status
    }

    @Override
    public MachineRealtimeRespVO getLatestByVendorDeviceId(String vendorDeviceId) {
        if (!StringUtils.hasText(vendorDeviceId)) {
            return null;
        }
        MachineRealtimeDO dobj = machineRealtimeMapper.selectByVendorDeviceId(vendorDeviceId.trim());
        if (dobj == null) {
            return null;
        }
        MachineRealtimeRespVO respVO = BeanUtils.toBean(dobj, MachineRealtimeRespVO.class);
        fillLoomInfo(Collections.singletonList(respVO));
        return respVO;
    }

    @Override
    public MachineRealtimeRespVO getLatestByLoomId(Long loomId) {
        if (loomId == null) {
            return null;
        }
        DeviceBindingDO binding = deviceBindingMapper.selectByLoomId(loomId);
        if (binding == null || !StringUtils.hasText(binding.getVendorDeviceId())) {
            return null;
        }
        MachineRealtimeDO dobj = machineRealtimeMapper.selectByVendorDeviceId(binding.getVendorDeviceId());
        if (dobj == null) {
            return null;
        }
        MachineRealtimeRespVO respVO = BeanUtils.toBean(dobj, MachineRealtimeRespVO.class);
        fillLoomInfo(Collections.singletonList(respVO));
        return respVO;
    }

    @Override
    public PageResult<MachineRealtimeRespVO> getMachineRealtimePage(MachineRealtimePageReqVO pageReqVO) {
        PageResult<MachineRealtimeDO> pageResult = machineRealtimeMapper.selectPage(pageReqVO);
        List<MachineRealtimeRespVO> list = BeanUtils.toBean(pageResult.getList(), MachineRealtimeRespVO.class);
        fillLoomInfo(list);
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public int triggerSimulatedCollection() {
        List<MachineRealtimeDO> snapshots = loomDataCollector.collectAll();
        if (snapshots == null || snapshots.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MachineRealtimeDO snapshot : snapshots) {
            if (snapshot == null || !StringUtils.hasText(snapshot.getVendorDeviceId())) {
                continue;
            }
            MachineRealtimeDO existing = machineRealtimeMapper.selectByVendorDeviceId(snapshot.getVendorDeviceId());
            if (existing != null) {
                snapshot.setId(existing.getId());
                machineRealtimeMapper.updateById(snapshot);
            } else {
                machineRealtimeMapper.insert(snapshot);
            }

            // 更新对应绑定的最后同步时间
            DeviceBindingDO binding = deviceBindingMapper.selectByVendorDeviceId(snapshot.getVendorDeviceId());
            if (binding != null) {
                binding.setLastSyncAt(snapshot.getFetchedAt());
                deviceBindingMapper.updateById(binding);
            }
            count++;
        }
        return count;
    }

    private void fillLoomInfo(List<MachineRealtimeRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<String> vendorDeviceIds = list.stream()
                .map(MachineRealtimeRespVO::getVendorDeviceId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (vendorDeviceIds.isEmpty()) {
            return;
        }

        // 批量查绑定
        List<DeviceBindingDO> bindings = deviceBindingMapper.selectList(DeviceBindingDO::getVendorDeviceId, vendorDeviceIds);
        Map<String, DeviceBindingDO> bindingMap = bindings.stream()
                .collect(Collectors.toMap(DeviceBindingDO::getVendorDeviceId, b -> b, (a, b) -> a));

        Set<Long> loomIds = bindingMap.values().stream()
                .map(DeviceBindingDO::getLoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, LoomDO> loomMap = Collections.emptyMap();
        if (!loomIds.isEmpty()) {
            List<LoomDO> looms = loomMapper.selectBatchIds(loomIds);
            loomMap = looms.stream().collect(Collectors.toMap(LoomDO::getId, l -> l, (a, b) -> a));
        }

        for (MachineRealtimeRespVO item : list) {
            DeviceBindingDO binding = bindingMap.get(item.getVendorDeviceId());
            if (binding != null) {
                item.setLoomId(binding.getLoomId());
                LoomDO loom = loomMap.get(binding.getLoomId());
                if (loom != null) {
                    item.setLoomNo(loom.getLoomNo());
                }
            }
        }
    }

}

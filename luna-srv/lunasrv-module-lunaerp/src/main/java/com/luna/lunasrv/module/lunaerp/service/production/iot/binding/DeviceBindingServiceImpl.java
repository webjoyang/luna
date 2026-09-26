package com.luna.lunasrv.module.lunaerp.service.production.iot.binding;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.DeviceBindingMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - IoT 设备绑定 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class DeviceBindingServiceImpl implements DeviceBindingService {

    @Resource
    private DeviceBindingMapper deviceBindingMapper;

    @Resource
    private LoomMapper loomMapper;

    @Override
    public Long createDeviceBinding(DeviceBindingSaveReqVO createReqVO) {
        // 校验织机存在
        validateLoomExists(createReqVO.getLoomId());

        // 校验设备与织机未被重复绑定（同设备重复绑定报 411）
        validateBindingUnique(null, createReqVO.getVendorDeviceId(), createReqVO.getLoomId());

        DeviceBindingDO bindingDO = BeanUtils.toBean(createReqVO, DeviceBindingDO.class);
        if (!StringUtils.hasText(bindingDO.getStatus())) {
            bindingDO.setStatus("ACTIVE");
        }
        deviceBindingMapper.insert(bindingDO);
        return bindingDO.getId();
    }

    @Override
    public void updateDeviceBinding(DeviceBindingSaveReqVO updateReqVO) {
        // 校验存在
        DeviceBindingDO existing = validateDeviceBindingExists(updateReqVO.getId());
        // 校验织机存在
        validateLoomExists(updateReqVO.getLoomId());
        // 校验唯一
        validateBindingUnique(updateReqVO.getId(), updateReqVO.getVendorDeviceId(), updateReqVO.getLoomId());

        existing.setVendorDeviceId(updateReqVO.getVendorDeviceId());
        existing.setLoomId(updateReqVO.getLoomId());
        if (StringUtils.hasText(updateReqVO.getStatus())) {
            existing.setStatus(updateReqVO.getStatus());
        }
        deviceBindingMapper.updateById(existing);
    }

    @Override
    public void deleteDeviceBinding(Long id) {
        validateDeviceBindingExists(id);
        deviceBindingMapper.deleteById(id);
    }

    @Override
    public DeviceBindingRespVO getDeviceBinding(Long id) {
        DeviceBindingDO dobj = validateDeviceBindingExists(id);
        DeviceBindingRespVO respVO = BeanUtils.toBean(dobj, DeviceBindingRespVO.class);
        fillLoomInfo(Collections.singletonList(respVO));
        return respVO;
    }

    @Override
    public PageResult<DeviceBindingRespVO> getDeviceBindingPage(DeviceBindingPageReqVO pageReqVO) {
        PageResult<DeviceBindingDO> pageResult = deviceBindingMapper.selectPage(pageReqVO);
        List<DeviceBindingRespVO> list = BeanUtils.toBean(pageResult.getList(), DeviceBindingRespVO.class);
        fillLoomInfo(list);
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public DeviceBindingRespVO getByLoomId(Long loomId) {
        if (loomId == null) {
            return null;
        }
        DeviceBindingDO dobj = deviceBindingMapper.selectByLoomId(loomId);
        if (dobj == null) {
            return null;
        }
        DeviceBindingRespVO respVO = BeanUtils.toBean(dobj, DeviceBindingRespVO.class);
        fillLoomInfo(Collections.singletonList(respVO));
        return respVO;
    }

    private DeviceBindingDO validateDeviceBindingExists(Long id) {
        if (id == null) {
            throw exception(IOT_DEVICE_BINDING_NOT_EXISTS);
        }
        DeviceBindingDO dobj = deviceBindingMapper.selectById(id);
        if (dobj == null) {
            throw exception(IOT_DEVICE_BINDING_NOT_EXISTS);
        }
        return dobj;
    }

    private void validateLoomExists(Long loomId) {
        if (loomId == null) {
            throw exception(LOOM_NOT_EXISTS);
        }
        LoomDO loomDO = loomMapper.selectById(loomId);
        if (loomDO == null) {
            throw exception(LOOM_NOT_EXISTS);
        }
    }

    private void validateBindingUnique(Long id, String vendorDeviceId, Long loomId) {
        if (StringUtils.hasText(vendorDeviceId)) {
            DeviceBindingDO existingDev = deviceBindingMapper.selectByVendorDeviceId(vendorDeviceId.trim());
            if (existingDev != null && (id == null || !existingDev.getId().equals(id))) {
                throw exception(IOT_DEVICE_ALREADY_BOUND, vendorDeviceId);
            }
        }
        if (loomId != null) {
            DeviceBindingDO existingLoom = deviceBindingMapper.selectByLoomId(loomId);
            if (existingLoom != null && (id == null || !existingLoom.getId().equals(id))) {
                throw exception(IOT_LOOM_ALREADY_BOUND, existingLoom.getVendorDeviceId());
            }
        }
    }

    private void fillLoomInfo(java.util.List<DeviceBindingRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.Set<Long> loomIds = list.stream()
                .map(DeviceBindingRespVO::getLoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (loomIds.isEmpty()) {
            return;
        }
        java.util.List<LoomDO> looms = loomMapper.selectBatchIds(loomIds);
        Map<Long, LoomDO> loomMap = looms.stream().collect(Collectors.toMap(LoomDO::getId, l -> l, (a, b) -> a));
        for (DeviceBindingRespVO item : list) {
            if (item.getLoomId() != null) {
                LoomDO loom = loomMap.get(item.getLoomId());
                if (loom != null) {
                    item.setLoomNo(loom.getLoomNo());
                    item.setLoomModel(loom.getModel());
                }
            }
        }
    }

}

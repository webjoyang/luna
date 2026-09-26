package com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产管理 - IoT 设备绑定 Mapper
 *
 * @author luna
 */
@Mapper
public interface DeviceBindingMapper extends BaseMapperX<DeviceBindingDO> {

    default PageResult<DeviceBindingDO> selectPage(DeviceBindingPageReqVO reqVO) {
        LambdaQueryWrapperX<DeviceBindingDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(DeviceBindingDO::getVendorDeviceId, reqVO.getVendorDeviceId());
        wrapper.eqIfPresent(DeviceBindingDO::getLoomId, reqVO.getLoomId());
        wrapper.eqIfPresent(DeviceBindingDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.apply("LOWER(vendor_device_id) LIKE LOWER({0})", like);
        }
        wrapper.orderByDesc(DeviceBindingDO::getCreateTime).orderByDesc(DeviceBindingDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default DeviceBindingDO selectByVendorDeviceId(String vendorDeviceId) {
        return selectOne(DeviceBindingDO::getVendorDeviceId, vendorDeviceId);
    }

    default DeviceBindingDO selectByLoomId(Long loomId) {
        return selectOne(DeviceBindingDO::getLoomId, loomId);
    }

    default List<DeviceBindingDO> selectListByStatus(String status) {
        return selectList(DeviceBindingDO::getStatus, status);
    }

}

package com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.MachineRealtimeDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * 生产管理 - IoT 设备实时快照 Mapper
 *
 * @author luna
 */
@Mapper
public interface MachineRealtimeMapper extends BaseMapperX<MachineRealtimeDO> {

    default PageResult<MachineRealtimeDO> selectPage(MachineRealtimePageReqVO reqVO) {
        LambdaQueryWrapperX<MachineRealtimeDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(MachineRealtimeDO::getVendorDeviceId, reqVO.getVendorDeviceId());
        wrapper.eqIfPresent(MachineRealtimeDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(vendor_device_id) LIKE LOWER({0})", like)
                    .or().apply("LOWER(device_sn) LIKE LOWER({0})", like)
                    .or().apply("LOWER(staff_on_shift) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(MachineRealtimeDO::getFetchedAt).orderByDesc(MachineRealtimeDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default MachineRealtimeDO selectByVendorDeviceId(String vendorDeviceId) {
        return selectOne(MachineRealtimeDO::getVendorDeviceId, vendorDeviceId);
    }

}

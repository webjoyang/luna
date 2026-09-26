package com.luna.lunasrv.module.lunaerp.service.production.iot.binding;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingSaveReqVO;
import jakarta.validation.Valid;

/**
 * 生产管理 - IoT 设备绑定 Service 接口
 *
 * @author luna
 */
public interface DeviceBindingService {

    /**
     * 创建设备绑定
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceBinding(@Valid DeviceBindingSaveReqVO createReqVO);

    /**
     * 更新设备绑定
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceBinding(@Valid DeviceBindingSaveReqVO updateReqVO);

    /**
     * 删除设备绑定
     *
     * @param id 编号
     */
    void deleteDeviceBinding(Long id);

    /**
     * 获得设备绑定详情
     *
     * @param id 编号
     * @return 设备绑定
     */
    DeviceBindingRespVO getDeviceBinding(Long id);

    /**
     * 获得设备绑定分页
     *
     * @param pageReqVO 分页查询
     * @return 设备绑定分页
     */
    PageResult<DeviceBindingRespVO> getDeviceBindingPage(DeviceBindingPageReqVO pageReqVO);

    /**
     * 根据织机ID查询绑定
     *
     * @param loomId 织机ID
     * @return 设备绑定
     */
    DeviceBindingRespVO getByLoomId(Long loomId);

}

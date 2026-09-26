package com.luna.lunasrv.module.lunaerp.service.production.iot.realtime;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeSaveReqVO;
import jakarta.validation.Valid;

/**
 * 生产管理 - IoT 设备实时快照 Service 接口
 *
 * @author luna
 */
public interface MachineRealtimeService {

    /**
     * 手工录入/更新实时快照（更新覆盖最新值，不回写 loom.status）
     *
     * @param saveReqVO 快照信息
     * @return 记录编号
     */
    Long recordRealtimeData(@Valid MachineRealtimeSaveReqVO saveReqVO);

    /**
     * 根据厂商设备标识查询最新快照
     *
     * @param vendorDeviceId 厂商设备标识
     * @return 实时快照
     */
    MachineRealtimeRespVO getLatestByVendorDeviceId(String vendorDeviceId);

    /**
     * 根据织机ID查询最新快照（通过绑定关系权威路由）
     *
     * @param loomId 织机ID
     * @return 实时快照
     */
    MachineRealtimeRespVO getLatestByLoomId(Long loomId);

    /**
     * 获得实时快照分页
     *
     * @param pageReqVO 分页查询
     * @return 快照分页
     */
    PageResult<MachineRealtimeRespVO> getMachineRealtimePage(MachineRealtimePageReqVO pageReqVO);

    /**
     * 手动触发模拟采集（根据有效绑定生成并覆盖实时快照）
     *
     * @return 采集更新的设备台数
     */
    int triggerSimulatedCollection();

}

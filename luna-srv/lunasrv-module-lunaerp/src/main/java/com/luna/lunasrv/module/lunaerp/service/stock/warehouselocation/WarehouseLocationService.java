package com.luna.lunasrv.module.lunaerp.service.stock.warehouselocation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 库存管理 - 库位 Service 接口
 *
 * @author luna
 */
public interface WarehouseLocationService {

    /**
     * 创建库位
     *
     * @param createReqVO 创建信息
     * @return 库位ID
     */
    Long createWarehouseLocation(@Valid WarehouseLocationSaveReqVO createReqVO);

    /**
     * 更新库位
     *
     * @param updateReqVO 更新信息
     */
    void updateWarehouseLocation(@Valid WarehouseLocationSaveReqVO updateReqVO);

    /**
     * 删除库位（带守卫：被放置记录引用禁止删除）
     *
     * @param id 库位ID
     */
    void deleteWarehouseLocation(Long id);

    /**
     * 获得库位详情
     *
     * @param id 库位ID
     * @return 库位详情
     */
    WarehouseLocationRespVO getWarehouseLocation(Long id);

    /**
     * 获得库位分页列表
     *
     * @param pageReqVO 分页查询条件
     * @return 库位分页结果
     */
    PageResult<WarehouseLocationRespVO> getWarehouseLocationPage(WarehouseLocationPageReqVO pageReqVO);

    /**
     * 获得指定仓库下的启用库位列表（供下拉选择使用，默认仅查询 ACTIVE 启用状态）
     *
     * @param warehouseId 仓库ID
     * @param status 状态过滤，可空（为空时默认查 ACTIVE）
     * @return 库位列表
     */
    List<WarehouseLocationRespVO> getWarehouseLocationListByWarehouse(Long warehouseId, String status);

}

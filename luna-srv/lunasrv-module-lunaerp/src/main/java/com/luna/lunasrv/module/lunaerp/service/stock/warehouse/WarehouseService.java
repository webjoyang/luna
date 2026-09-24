package com.luna.lunasrv.module.lunaerp.service.stock.warehouse;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehousePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 库存管理 - 仓库 Service 接口
 *
 * @author luna
 */
public interface WarehouseService {

    /**
     * 创建仓库
     *
     * @param createReqVO 创建信息
     * @return 仓库ID
     */
    Long createWarehouse(@Valid WarehouseSaveReqVO createReqVO);

    /**
     * 更新仓库
     *
     * @param updateReqVO 更新信息
     */
    void updateWarehouse(@Valid WarehouseSaveReqVO updateReqVO);

    /**
     * 删除仓库（带守卫：有库位或被入库单引用禁止删除）
     *
     * @param id 仓库ID
     */
    void deleteWarehouse(Long id);

    /**
     * 获得仓库详情
     *
     * @param id 仓库ID
     * @return 仓库详情
     */
    WarehouseRespVO getWarehouse(Long id);

    /**
     * 获得仓库分页列表
     *
     * @param pageReqVO 分页查询条件
     * @return 仓库分页结果
     */
    PageResult<WarehouseRespVO> getWarehousePage(WarehousePageReqVO pageReqVO);

    /**
     * 获得所有仓库简表（供下拉选择使用，默认仅查询 ACTIVE 启用状态）
     *
     * @param status 状态过滤，可空（为空时默认查 ACTIVE）
     * @return 仓库列表
     */
    List<WarehouseRespVO> getWarehouseListAll(String status);

    /**
     * 预览下一个仓库编号（WH+年份+4位流水，只读、不占号）
     *
     * @return 仓库编号预览
     */
    String getNextWarehouseNo();

}

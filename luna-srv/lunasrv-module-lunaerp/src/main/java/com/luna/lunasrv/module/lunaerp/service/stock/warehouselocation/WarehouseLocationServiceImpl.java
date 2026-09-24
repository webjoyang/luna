package com.luna.lunasrv.module.lunaerp.service.stock.warehouselocation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_CODE_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_IN_USE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;

/**
 * 库存管理 - 库位 Service 实现类
 *
 * 仓内编码唯一（含软删行，ADR-0018）。
 * 删除守卫：已被放置记录引用禁止删除，引导停用（1_030_013_102）。
 *
 * @author luna
 */
@Service
@Validated
public class WarehouseLocationServiceImpl implements WarehouseLocationService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouseLocation(WarehouseLocationSaveReqVO createReqVO) {
        // 1. 校验仓库存在
        if (createReqVO.getWarehouseId() == null || warehouseMapper.selectById(createReqVO.getWarehouseId()) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        // 2. 校验同仓库内编码唯一（含软删行）
        validateLocationCodeUnique(null, createReqVO.getWarehouseId(), createReqVO.getCode());

        WarehouseLocationDO location = BeanUtils.toBean(createReqVO, WarehouseLocationDO.class);
        if (!StringUtils.hasText(location.getStatus())) {
            location.setStatus(DEFAULT_STATUS);
        }
        if (location.getSort() == null) {
            location.setSort(0);
        }
        warehouseLocationMapper.insert(location);
        return location.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseLocation(WarehouseLocationSaveReqVO updateReqVO) {
        WarehouseLocationDO oldLocation = validateWarehouseLocationExists(updateReqVO.getId());
        Long warehouseId = updateReqVO.getWarehouseId() != null ? updateReqVO.getWarehouseId() : oldLocation.getWarehouseId();

        // 校验仓库存在
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }

        // 校验同仓库内编码唯一（排除自身，含软删行）
        if (StringUtils.hasText(updateReqVO.getCode())) {
            validateLocationCodeUnique(updateReqVO.getId(), warehouseId, updateReqVO.getCode());
        }

        WarehouseLocationDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseLocationDO.class);
        warehouseLocationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseLocation(Long id) {
        validateWarehouseLocationExists(id);

        // 删除守卫：库位被放置记录引用禁止删除，引导改用停用
        Long placementCount = stockInItemPlacementMapper.selectCountByLocationId(id);
        if (placementCount != null && placementCount > 0) {
            throw exception(WAREHOUSE_LOCATION_IN_USE);
        }

        // 先置 status='INACTIVE'，再逻辑删除
        WarehouseLocationDO updateObj = new WarehouseLocationDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        warehouseLocationMapper.updateById(updateObj);

        warehouseLocationMapper.deleteById(id);
    }

    @Override
    public WarehouseLocationRespVO getWarehouseLocation(Long id) {
        WarehouseLocationDO location = warehouseLocationMapper.selectById(id);
        if (location == null) {
            throw exception(WAREHOUSE_LOCATION_NOT_EXISTS);
        }
        return BeanUtils.toBean(location, WarehouseLocationRespVO.class);
    }

    @Override
    public PageResult<WarehouseLocationRespVO> getWarehouseLocationPage(WarehouseLocationPageReqVO pageReqVO) {
        PageResult<WarehouseLocationDO> pageResult = warehouseLocationMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, WarehouseLocationRespVO.class);
    }

    @Override
    public List<WarehouseLocationRespVO> getWarehouseLocationListByWarehouse(Long warehouseId, String status) {
        if (warehouseId == null) {
            return List.of();
        }
        String queryStatus = StringUtils.hasText(status) ? status : DEFAULT_STATUS;
        List<WarehouseLocationDO> list = warehouseLocationMapper.selectList(new LambdaQueryWrapperX<WarehouseLocationDO>()
                .eq(WarehouseLocationDO::getWarehouseId, warehouseId)
                .eq(WarehouseLocationDO::getStatus, queryStatus)
                .orderByAsc(WarehouseLocationDO::getSort)
                .orderByAsc(WarehouseLocationDO::getCode));
        return BeanUtils.toBean(list, WarehouseLocationRespVO.class);
    }

    // ==================== 私有方法 ====================

    private WarehouseLocationDO validateWarehouseLocationExists(Long id) {
        if (id == null) {
            throw exception(WAREHOUSE_LOCATION_NOT_EXISTS);
        }
        WarehouseLocationDO location = warehouseLocationMapper.selectById(id);
        if (location == null) {
            throw exception(WAREHOUSE_LOCATION_NOT_EXISTS);
        }
        return location;
    }

    private void validateLocationCodeUnique(Long excludeId, Long warehouseId, String code) {
        if (!StringUtils.hasText(code) || warehouseId == null) {
            return;
        }
        WarehouseLocationDO exist = warehouseLocationMapper.selectByWarehouseIdAndCodeIncludingDeleted(warehouseId, code.trim());
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(WAREHOUSE_LOCATION_CODE_DUPLICATE, code);
        }
    }

}

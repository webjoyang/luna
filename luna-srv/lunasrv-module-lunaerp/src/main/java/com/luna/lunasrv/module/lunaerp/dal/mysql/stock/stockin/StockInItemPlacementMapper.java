package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemPlacementDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存管理 - 入库明细库位放置记录 Mapper
 *
 * @author luna
 */
@Mapper
public interface StockInItemPlacementMapper extends BaseMapperX<StockInItemPlacementDO> {

    /**
     * 按库位ID统计未删除的放置记录数量（用于库位删除守卫）
     *
     * @param locationId 库位ID
     * @return 引用该库位的放置记录数量
     */
    default Long selectCountByLocationId(Long locationId) {
        return selectCount(StockInItemPlacementDO::getLocationId, locationId);
    }

}

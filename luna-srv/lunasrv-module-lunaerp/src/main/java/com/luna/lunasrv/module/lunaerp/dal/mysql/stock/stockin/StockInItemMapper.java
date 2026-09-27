package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 库存管理 - 入库明细 Mapper
 *
 * @author luna
 */
@Mapper
public interface StockInItemMapper extends BaseMapperX<StockInItemDO> {

    /**
     * 按入库单ID查询明细列表（按 id 升序）
     *
     * @param stockInId 入库单ID
     * @return 明细列表
     */
    default List<StockInItemDO> selectListByStockInId(Long stockInId) {
        return selectList(StockInItemDO::getStockInId, stockInId);
    }

    /**
     * 按入库单ID逻辑删除全部明细
     *
     * @param stockInId 入库单ID
     */
    default void deleteByStockInId(Long stockInId) {
        delete(StockInItemDO::getStockInId, stockInId);
    }

    /**
     * 按检验明细ID列表查询入库明细
     *
     * @param inspectionOrderItemIds 检验明细ID列表
     * @return 入库明细列表
     */
    default List<StockInItemDO> selectListByInspectionOrderItemIds(java.util.Collection<Long> inspectionOrderItemIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(inspectionOrderItemIds)) {
            return java.util.Collections.emptyList();
        }
        return selectList(new com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX<StockInItemDO>()
                .in(StockInItemDO::getInspectionOrderItemId, inspectionOrderItemIds));
    }

}

package com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 业务管理 - 发货明细 Mapper
 *
 * @author luna
 */
@Mapper
public interface ShipmentItemMapper extends BaseMapperX<ShipmentItemDO> {

    /**
     * 按发货单ID查询明细列表（按 id 升序）
     *
     * @param shipmentId 发货单ID
     * @return 明细列表
     */
    default List<ShipmentItemDO> selectListByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .eq(ShipmentItemDO::getShipmentId, shipmentId)
                .orderByAsc(ShipmentItemDO::getId));
    }

    /**
     * 按发货单ID列表查询明细列表
     *
     * @param shipmentIds 发货单ID列表
     * @return 明细列表
     */
    default List<ShipmentItemDO> selectListByShipmentIds(Collection<Long> shipmentIds) {
        if (CollUtil.isEmpty(shipmentIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .in(ShipmentItemDO::getShipmentId, shipmentIds));
    }

    /**
     * 按发货单ID逻辑删除全部明细
     *
     * @param shipmentId 发货单ID
     */
    default void deleteByShipmentId(Long shipmentId) {
        delete(ShipmentItemDO::getShipmentId, shipmentId);
    }

}

package com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 业务管理 - 发货卷清单 Mapper
 *
 * @author luna
 */
@Mapper
public interface ShipmentRollMapper extends BaseMapperX<ShipmentRollDO> {

    /**
     * 级联恢复发货卷清单（手写 XML：显式 SET deleted=0 WHERE shipment_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByShipmentId(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除发货卷清单（手写 XML：显式 DELETE WHERE shipment_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByShipmentId(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId);

    /**
     * 按发货单ID查询卷清单列表
     *
     * @param shipmentId 发货单ID
     * @return 卷清单列表
     */
    default List<ShipmentRollDO> selectListByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<ShipmentRollDO>()
                .eq(ShipmentRollDO::getShipmentId, shipmentId)
                .orderByAsc(ShipmentRollDO::getId));
    }

    /**
     * 按发货明细ID查询卷清单列表
     *
     * @param shipmentItemId 发货明细ID
     * @return 卷清单列表
     */
    default List<ShipmentRollDO> selectListByShipmentItemId(Long shipmentItemId) {
        return selectList(ShipmentRollDO::getShipmentItemId, shipmentItemId);
    }

    /**
     * 按发货单ID列表查询卷清单列表
     *
     * @param shipmentIds 发货单ID列表
     * @return 卷清单列表
     */
    default List<ShipmentRollDO> selectListByShipmentIds(Collection<Long> shipmentIds) {
        if (CollUtil.isEmpty(shipmentIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ShipmentRollDO>()
                .in(ShipmentRollDO::getShipmentId, shipmentIds));
    }

    /**
     * 按发货单ID逻辑删除全部卷清单
     *
     * @param shipmentId 发货单ID
     */
    default void deleteByShipmentId(Long shipmentId) {
        delete(ShipmentRollDO::getShipmentId, shipmentId);
    }

    /**
     * 按发货明细ID逻辑删除卷清单
     *
     * @param shipmentItemId 发货明细ID
     */
    default void deleteByShipmentItemId(Long shipmentItemId) {
        delete(ShipmentRollDO::getShipmentItemId, shipmentItemId);
    }

}

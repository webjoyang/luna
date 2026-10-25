package com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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
     * 查询已删除发货单下的明细（用于回收站恢复前主数据引用校验）
     */
    List<ShipmentItemDO> selectDeletedListByShipmentId(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId);

    /**
     * 级联恢复发货明细（手写 XML：显式 SET deleted=0 WHERE shipment_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByShipmentId(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除发货明细（手写 XML：显式 DELETE WHERE shipment_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByShipmentId(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId);

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

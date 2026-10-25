package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产管理-纱线采购明细表 Mapper
 *
 * @author luna
 */
@Mapper
public interface YarnOrderItemMapper extends BaseMapperX<YarnOrderItemDO> {

    /**
     * 查询已删除采购单下的明细（用于回收站恢复前主数据引用校验）
     */
    List<YarnOrderItemDO> selectDeletedListByOrderId(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);

    /**
     * 级联恢复采购明细（手写 XML：显式 SET deleted=0 WHERE order_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByOrderId(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId,
                         @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除采购明细（手写 XML：显式 DELETE WHERE order_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByOrderId(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);

    default List<YarnOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .eq(YarnOrderItemDO::getOrderId, orderId)
                .orderByAsc(YarnOrderItemDO::getYarnSpecId));
    }

    /**
     * 查询指定采购单下所有明细的 ID 集合，用于差异保存时判断 ID 归属。
     */
    default Set<Long> selectIdsByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .eq(YarnOrderItemDO::getOrderId, orderId)
                .select(YarnOrderItemDO::getId))
                .stream()
                .map(YarnOrderItemDO::getId)
                .collect(Collectors.toSet());
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .eq(YarnOrderItemDO::getOrderId, orderId));
    }

    /**
     * 逻辑删除指定 ID 集合的明细行，用于差异保存时删除已移除的行。
     */
    default int deleteByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .in(YarnOrderItemDO::getId, ids));
    }

    /**
     * 查询某纱线规格在已下达（ACTIVE/COMPLETED）采购单中的最近单价
     *
     * @param yarnSpecId 纱线规格ID
     * @return 最近单价，无记录返回 null
     */
    @Select("SELECT unit_price FROM lunaerp_production_yarn_order_item "
            + "WHERE yarn_spec_id = #{yarnSpecId} AND deleted = 0 AND unit_price IS NOT NULL "
            + "AND order_id IN (SELECT id FROM lunaerp_production_yarn_order WHERE status IN ('ACTIVE', 'COMPLETED') AND deleted = 0) "
            + "ORDER BY update_time DESC, id DESC LIMIT 1")
    BigDecimal selectRecentUnitPriceBySpecId(@Param("yarnSpecId") Long yarnSpecId);

    /**
     * 批量按纱线规格ID集合聚合 ACTIVE 采购单在途采购数量（SUM(purchase_qty)）
     *
     * @param yarnSpecIds 纱线规格ID集合
     * @return 聚合列表（包含 yarnSpecId 与 purchaseQty）
     */
    @Select("<script>"
            + "SELECT i.yarn_spec_id AS yarnSpecId, COALESCE(SUM(i.purchase_qty), 0) AS purchaseQty "
            + "FROM lunaerp_production_yarn_order_item i "
            + "JOIN lunaerp_production_yarn_order o ON o.id = i.order_id "
            + "WHERE i.deleted = 0 AND o.deleted = 0 "
            + "AND o.status = 'ACTIVE' "
            + "AND i.yarn_spec_id IN "
            + "<foreach item='id' collection='yarnSpecIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY i.yarn_spec_id"
            + "</script>")
    List<YarnOrderItemDO> sumInTransitQtyByYarnSpecIds(
            @Param("yarnSpecIds") java.util.Collection<Long> yarnSpecIds);

}

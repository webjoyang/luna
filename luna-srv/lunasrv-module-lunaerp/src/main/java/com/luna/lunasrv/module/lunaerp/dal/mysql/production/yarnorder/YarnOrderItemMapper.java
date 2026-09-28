package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
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

}

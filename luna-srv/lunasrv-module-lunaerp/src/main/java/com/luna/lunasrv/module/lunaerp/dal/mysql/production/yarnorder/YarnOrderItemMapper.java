package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

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
                .orderByAsc(YarnOrderItemDO::getId));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .eq(YarnOrderItemDO::getOrderId, orderId));
    }

    /**
     * 查询某纱线规格在已下达（ACTIVE/COMPLETED）采购单中的最近单价
     *
     * @param yarnSpecId 纱线规格ID
     * @return 最近单价，无记录返回 null
     */
    @Select("SELECT i.unit_price FROM lunaerp_production_yarn_order_item i "
            + "INNER JOIN lunaerp_production_yarn_order o ON i.order_id = o.id "
            + "WHERE i.yarn_spec_id = #{yarnSpecId} AND i.deleted = 0 AND o.deleted = 0 "
            + "AND o.status IN ('ACTIVE', 'COMPLETED') AND i.unit_price IS NOT NULL "
            + "ORDER BY o.order_date DESC, i.id DESC LIMIT 1")
    BigDecimal selectRecentUnitPriceBySpecId(@Param("yarnSpecId") Long yarnSpecId);

}

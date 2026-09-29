package com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-染色单颜色明细子表 Mapper
 *
 * @author luna
 */
@Mapper
public interface DyeingColorMapper extends BaseMapperX<DyeingColorDO> {

    default List<DyeingColorDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<DyeingColorDO>()
                .eq(DyeingColorDO::getDyeingOrderId, orderId)
                .orderByAsc(DyeingColorDO::getLineNo));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<DyeingColorDO>()
                .eq(DyeingColorDO::getDyeingOrderId, orderId));
    }

    /**
     * 按染色单ID列表查询颜色明细，【包含已逻辑删除的行】（用于进度反馈"含已失效"筛选）。
     *
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param orderIds 染色单ID集合
     * @return 颜色明细列表（含已删除行）
     */
    @org.apache.ibatis.annotations.Select("<script>"
            + "SELECT id, dyeing_order_id, line_no, customer_color_no, color_name, "
            + "quantity, pieces, dyeing_input_qty, dyeing_input_unit, contract_qty, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_dyeing_color "
            + "WHERE dyeing_order_id IN "
            + "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY line_no ASC"
            + "</script>")
    java.util.List<DyeingColorDO> selectListByOrderIdsIncludingDeleted(@org.apache.ibatis.annotations.Param("orderIds") java.util.Collection<Long> orderIds);

    /**
     * 按染色单ID集合批量查询颜色明细（未删除，生产进度聚合用，ADR-0034）。
     *
     * <p>返回未加工列表，KG/M 口径过滤与投染量、覆盖率分母求和由 Service 内存完成，便于单测。</p>
     *
     * @param orderIds 染色单ID集合
     * @return 颜色明细列表
     */
    default List<DyeingColorDO> selectListByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DyeingColorDO>()
                .in(DyeingColorDO::getDyeingOrderId, orderIds));
    }

}

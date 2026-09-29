package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-织造执行-织造单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface WeavingOrderMapper extends BaseMapperX<WeavingOrderDO> {

    /**
     * 按单号前缀查询织造单，【包含已逻辑删除的行】。
     *
     * ADR-0023 / ADR-0004 / ADR-0006：单号租户内全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单据的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param prefix 单号前缀，如 ZZ-PB0919001-
     * @return 织造单列表（含已删除行）
     */
    @Select("SELECT id, code, greige_order_id, greige_order_no, contract_no, product_id, product_code, product_name, "
            + "composition, gsm, width, planned_qty, planned_weight, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_weaving_order WHERE code LIKE CONCAT(#{prefix}, '%')")
    List<WeavingOrderDO> selectListByCodePrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询织造单，【包含已逻辑删除的行】。
     *
     * @param code 织造单号
     * @return 织造单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, code, greige_order_id, greige_order_no, contract_no, product_id, product_code, product_name, "
            + "composition, gsm, width, planned_qty, planned_weight, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_weaving_order WHERE code = #{code} LIMIT 1")
    WeavingOrderDO selectByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 获取指定胚布单下所有有效织造单（未软删且状态不为 CANCELLED）
     *
     * @param greigeOrderId 胚布单ID
     * @return 有效织造单列表
     */
    default List<WeavingOrderDO> selectActiveListByGreigeOrderId(Long greigeOrderId) {
        return selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .eq(WeavingOrderDO::getGreigeOrderId, greigeOrderId)
                .ne(WeavingOrderDO::getStatus, "CANCELLED"));
    }

    /**
     * 获取指定胚布单下所有织造单（未软删）
     *
     * @param greigeOrderId 胚布单ID
     * @return 织造单列表
     */
    default List<WeavingOrderDO> selectListByGreigeOrderId(Long greigeOrderId) {
        return selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .eq(WeavingOrderDO::getGreigeOrderId, greigeOrderId));
    }

    /**
     * 按胚布单ID集合批量查询织造单（生产进度自织进度聚合用，ADR-0034）。
     *
     * <p>返回未加工列表（含各状态），CANCELLED 排除与完工口径由 Service 内存完成，便于单测。</p>
     *
     * @param greigeOrderIds 胚布单ID集合
     * @return 织造单列表
     */
    default List<WeavingOrderDO> selectListByGreigeOrderIds(Collection<Long> greigeOrderIds) {
        if (greigeOrderIds == null || greigeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .in(WeavingOrderDO::getGreigeOrderId, greigeOrderIds));
    }

}

package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产管理-纱线采购单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface YarnOrderMapper extends BaseMapperX<YarnOrderDO> {

    /**
     * 按单号前缀查询采购单，【包含已逻辑删除的行】。
     *
     * 采购单号 (tenant_id, order_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单据的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。
     *
     * @param prefix 单号前缀，如 YP20260920
     * @return 采购单列表（含已删除行）
     */
    @Select("SELECT id, order_no, source_instructions, order_date, delivery_date, loss_rate, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    List<YarnOrderDO> selectListByOrderNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询采购单，【包含已逻辑删除的行】。
     *
     * 手填单号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新单号应报 YARN_ORDER_NO_DUPLICATE 而非放行。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param orderNo 采购单号
     * @return 采购单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, order_no, source_instructions, order_date, delivery_date, loss_rate, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_order WHERE order_no = #{orderNo} LIMIT 1")
    YarnOrderDO selectByOrderNoIncludingDeleted(@Param("orderNo") String orderNo);

}

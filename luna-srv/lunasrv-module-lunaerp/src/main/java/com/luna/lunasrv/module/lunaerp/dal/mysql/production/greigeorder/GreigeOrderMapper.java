package com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产管理-胚布单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface GreigeOrderMapper extends BaseMapperX<GreigeOrderDO> {

    /**
     * 按单号前缀查询胚布单，【包含已逻辑删除的行】。
     *
     * ADR-0006：胚布单号 (tenant_id, order_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单据的编号会被重新分配。
     * 因此此处用手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤（deleted=0 条件不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。勿"修复"为 wrapper 查询。
     *
     * @param prefix 单号前缀，如 PB0919
     * @return 胚布单列表（含已删除行）
     */
    @Select("SELECT id, order_no, sales_contract_id, contract_no, greige_factory_id, greige_factory_name, "
            + "dyeing_factory_id, dyeing_factory_name, order_date, delivery_date, product_code, product_name, "
            + "composition, width, gsm, shrinkage, piece_weight, piece_count, planned_qty, finished_qty, "
            + "suggested_loss, processing_fee, greige_type_id, production_remark, remark, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_greige_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    List<GreigeOrderDO> selectListByOrderNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询胚布单，【包含已逻辑删除的行】。
     *
     * ADR-0006：手填单号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新单号应报 GREIGE_ORDER_NO_DUPLICATE 而非放行。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param orderNo 胚布单号
     * @return 胚布单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, order_no, sales_contract_id, contract_no, greige_factory_id, greige_factory_name, "
            + "dyeing_factory_id, dyeing_factory_name, order_date, delivery_date, product_code, product_name, "
            + "composition, width, gsm, shrinkage, piece_weight, piece_count, planned_qty, finished_qty, "
            + "suggested_loss, processing_fee, greige_type_id, production_remark, remark, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_greige_order WHERE order_no = #{orderNo} LIMIT 1")
    GreigeOrderDO selectByOrderNoIncludingDeleted(@Param("orderNo") String orderNo);

}

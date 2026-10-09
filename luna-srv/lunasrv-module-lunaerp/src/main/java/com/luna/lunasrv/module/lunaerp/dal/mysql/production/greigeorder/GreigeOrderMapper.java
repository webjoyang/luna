package com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-胚布单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface GreigeOrderMapper extends BaseMapperX<GreigeOrderDO> {

    /**
     * 回收站分页查询已软删草稿胚布单（手写 XML：显式 WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿胚布单
     */
    GreigeOrderDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿胚布单（并发条件更新带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int restoreGreigeOrder(@Param("id") Long id, @Param("tenantId") Long tenantId,
                           @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除草稿胚布单（物理删除带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int purgeGreigeOrder(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查胚布单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("greigeOrderId") Long greigeOrderId, @Param("tenantId") Long tenantId);

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

    /**
     * 按销售合同ID集合批量查询胚布单（生产进度聚合用，ADR-0034）。
     *
     * <p>返回未加工列表（含各状态），CANCELLED 排除与 KG 口径由 Service 内存完成，便于单测。</p>
     *
     * @param contractIds 销售合同ID集合
     * @return 胚布单列表
     */
    default List<GreigeOrderDO> selectListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<GreigeOrderDO>()
                .in(GreigeOrderDO::getSalesContractId, contractIds));
    }

}

package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 库存管理 - 入库单 Mapper
 *
 * @author luna
 */
@Mapper
public interface StockInMapper extends BaseMapperX<StockInDO> {

    /**
     * 回收站分页查询已软删草稿入库单（手写 XML：显式 WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿入库单
     */
    StockInDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿入库单（并发条件更新带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int restoreStockIn(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除草稿入库单（物理删除带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int purgeStockIn(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查入库单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("stockInId") Long stockInId, @Param("tenantId") Long tenantId);

    /**
     * 按仓库ID统计未删除的入库单数量（用于仓库删除守卫）
     *
     * @param warehouseId 仓库ID
     * @return 引用该仓库的入库单数量
     */
    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(StockInDO::getWarehouseId, warehouseId);
    }

    /**
     * 按单号精确查询入库单（含已删除行，用于手填查重与自动号防撞）
     *
     * @param inNo 入库单号
     * @return 入库单（含已删除行）
     */
    @Select("SELECT id, in_no, in_type, status, warehouse_id, warehouse_name, supplier_id, supplier_name, "
            + "sales_contract_id, contract_no, greige_order_id, greige_order_no, dyeing_order_id, "
            + "greige_type_id, greige_type_name, total_amount, in_date, remark, creator, create_time, "
            + "updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_stock_in WHERE in_no = #{inNo} LIMIT 1")
    StockInDO selectByInNoIncludingDeleted(@Param("inNo") String inNo);

    /**
     * 按单号前缀查询入库单列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 IN-20260924-）
     * @return 入库单列表（含已删除行）
     */
    @Select("SELECT id, in_no, in_type, status, warehouse_id, warehouse_name, supplier_id, supplier_name, "
            + "sales_contract_id, contract_no, greige_order_id, greige_order_no, dyeing_order_id, "
            + "greige_type_id, greige_type_name, total_amount, in_date, remark, creator, create_time, "
            + "updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_stock_in WHERE in_no LIKE CONCAT(#{prefix}, '%')")
    List<StockInDO> selectListByInNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按销售合同ID集合批量查询入库单（生产进度入库量聚合用，ADR-0034）。
     *
     * <p>返回未加工列表（含各状态），CANCELLED 排除与胚布/成品区分由 Service 内存完成，便于单测。</p>
     *
     * @param contractIds 销售合同ID集合
     * @return 入库单列表
     */
    default List<StockInDO> selectListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<StockInDO>()
                .in(StockInDO::getSalesContractId, contractIds));
    }

}

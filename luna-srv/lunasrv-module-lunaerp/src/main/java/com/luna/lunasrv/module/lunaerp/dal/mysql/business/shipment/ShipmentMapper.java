package com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 业务管理 - 发货单 Mapper
 *
 * @author luna
 */
@Mapper
public interface ShipmentMapper extends BaseMapperX<ShipmentDO> {

    /**
     * 调岗同步归属部门（ADR-0046）
     *
     * <p>绕过数据权限规则，在当前租户下将该业务员名下所有有效发货单的归属部门刷新为新部门。</p>
     *
     * @param salespersonUserId 业务员用户编号
     * @param deptId 新部门编号
     * @return 更新行数
     */
    @DataPermission(enable = false)
    @Update("UPDATE lunaerp_business_shipment SET dept_id = #{deptId,jdbcType=BIGINT} "
            + "WHERE salesperson_user_id = #{salespersonUserId,jdbcType=BIGINT} AND deleted = 0")
    int updateDeptIdBySalespersonUserId(@Param("salespersonUserId") Long salespersonUserId,
                                        @Param("deptId") Long deptId);

    /**
     * 回收站分页查询已软删草稿发货单（手写 XML：显式 WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿发货单
     */
    ShipmentDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿发货单（并发条件更新带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int restoreShipment(@Param("id") Long id, @Param("tenantId") Long tenantId,
                        @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除草稿发货单（物理删除带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int purgeShipment(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查发货单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("shipmentId") Long shipmentId, @Param("tenantId") Long tenantId);

    /**
     * 按仓库ID统计未删除的发货单数量（用于仓库删除守卫）
     *
     * @param warehouseId 仓库ID
     * @return 引用该仓库的发货单数量
     */
    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ShipmentDO::getWarehouseId, warehouseId);
    }

    /**
     * 按客户ID统计未删除的发货单数量（用于客户删除守卫）
     *
     * @param customerId 客户ID
     * @return 引用该客户的发货单数量
     */
    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ShipmentDO::getCustomerId, customerId);
    }

    /**
     * 按单号精确查询发货单（含已删除行，用于手填查重与自动号防撞）
     *
     * @param shipmentNo 发货单号
     * @return 发货单（含已删除行）
     */
    @DataPermission(enable = false)
    @Select("SELECT id, shipment_no, status, customer_id, customer_name, sales_contract_id, contract_no, "
            + "warehouse_id, warehouse_name, shipping_address, receive_address, carrier, tracking_number, "
            + "shipping_date, expected_delivery_date, freight_fee, total_amount, total_pieces, "
            + "salesperson_user_id, salesperson_name, currency, sign_status, signed_by_name, signed_at, "
            + "sign_remark, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_shipment WHERE shipment_no = #{shipmentNo} LIMIT 1")
    ShipmentDO selectByShipmentNoIncludingDeleted(@Param("shipmentNo") String shipmentNo);

    /**
     * 按单号前缀查询发货单列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 SH-20260927-）
     * @return 发货单列表（含已删除行）
     */
    @DataPermission(enable = false)
    @Select("SELECT id, shipment_no, status, customer_id, customer_name, sales_contract_id, contract_no, "
            + "warehouse_id, warehouse_name, shipping_address, receive_address, carrier, tracking_number, "
            + "shipping_date, expected_delivery_date, freight_fee, total_amount, total_pieces, "
            + "salesperson_user_id, salesperson_name, currency, sign_status, signed_by_name, signed_at, "
            + "sign_remark, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_shipment WHERE shipment_no LIKE CONCAT(#{prefix}, '%')")
    List<ShipmentDO> selectListByShipmentNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按销售合同ID与状态查询发货单列表
     *
     * @param contractId 销售合同ID
     * @param status 发货单状态（如 COMPLETED）
     * @return 发货单列表
     */
    @DataPermission(enable = false)
    default List<ShipmentDO> selectListByContractIdAndStatus(Long contractId, String status) {
        return selectList(new LambdaQueryWrapperX<ShipmentDO>()
                .eq(ShipmentDO::getSalesContractId, contractId)
                .eq(ShipmentDO::getStatus, status));
    }

    /**
     * 按销售合同ID集合批量查询发货单（生产进度发货量聚合用，ADR-0034）。
     *
     * <p>返回未加工列表（含各状态），CANCELLED 排除由 Service 内存完成，便于单测。</p>
     *
     * @param contractIds 销售合同ID集合
     * @return 发货单列表
     */
    default List<ShipmentDO> selectListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ShipmentDO>()
                .in(ShipmentDO::getSalesContractId, contractIds));
    }

}

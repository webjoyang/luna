package com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务管理 - 发货单 Mapper
 *
 * @author luna
 */
@Mapper
public interface ShipmentMapper extends BaseMapperX<ShipmentDO> {

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
    @Select("SELECT id, shipment_no, status, customer_id, customer_name, sales_contract_id, contract_no, "
            + "warehouse_id, warehouse_name, shipping_address, receive_address, carrier, tracking_number, "
            + "shipping_date, expected_delivery_date, freight_fee, total_amount, total_pieces, "
            + "salesperson_user_id, salesperson_name, currency, sign_status, signed_by_name, signed_at, "
            + "sign_remark, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_shipment WHERE shipment_no LIKE CONCAT(#{prefix}, '%')")
    List<ShipmentDO> selectListByShipmentNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}

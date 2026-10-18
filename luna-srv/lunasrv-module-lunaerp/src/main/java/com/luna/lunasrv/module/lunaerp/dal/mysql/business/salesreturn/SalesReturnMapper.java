package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn;

import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务管理 - 销售退货主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SalesReturnMapper extends BaseMapperX<SalesReturnDO> {

    /**
     * 按单号精确查询销售退货单（含已删除行，用于手填查重与自动号防撞）
     *
     * @param tenantId 租户编号
     * @param returnNo 退货单号
     * @return 退货单（含已删除行）
     */
    @DataPermission(enable = false)
    @Select("SELECT id, return_no, status, approval_status, shipment_id, shipment_no, customer_id, customer_name, "
            + "sales_contract_id, contract_no, warehouse_id, warehouse_name, currency, total_amount, total_pieces, "
            + "stock_in_id, arrived_by, arrived_at, posted_by, posted_at, cancelled_by, cancelled_at, cancel_reason, "
            + "return_date, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_sales_return WHERE tenant_id = #{tenantId} AND return_no = #{returnNo} LIMIT 1")
    SalesReturnDO selectByReturnNoIncludingDeleted(@Param("tenantId") Long tenantId, @Param("returnNo") String returnNo);

    /**
     * 按单号前缀查询销售退货单列表（含已删除行，用于按天取号扫描）
     *
     * @param tenantId 租户编号
     * @param prefix 单号前缀（如 RT-20261018-）
     * @return 退货单列表（含已删除行）
     */
    @DataPermission(enable = false)
    @Select("SELECT id, return_no, status, approval_status, shipment_id, shipment_no, customer_id, customer_name, "
            + "sales_contract_id, contract_no, warehouse_id, warehouse_name, currency, total_amount, total_pieces, "
            + "stock_in_id, arrived_by, arrived_at, posted_by, posted_at, cancelled_by, cancelled_at, cancel_reason, "
            + "return_date, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_sales_return WHERE tenant_id = #{tenantId} AND return_no LIKE CONCAT(#{prefix}, '%')")
    List<SalesReturnDO> selectListByReturnNoPrefixIncludingDeleted(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

}

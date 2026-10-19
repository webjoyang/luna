package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn;

import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.dto.SalesReturnOccupiedQtyDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 业务管理 - 销售退货明细行 Mapper
 *
 * @author luna
 */
@Mapper
public interface SalesReturnItemMapper extends BaseMapperX<SalesReturnItemDO> {

    /**
     * 按发货明细行批量统计已占用的退货数量（仅统计未作废且审批中/已通过的退货单）
     *
     * @param tenantId 租户编号
     * @param shipmentItemIds 发货明细ID集合
     * @param excludeReturnId 排除的退货单ID（可空）
     * @return 占量统计列表
     */
    @DataPermission(enable = false)
    @Select("<script>"
            + "SELECT item.shipment_item_id AS shipmentItemId, COALESCE(SUM(item.return_qty), 0) AS occupiedQty "
            + "FROM lunaerp_business_sales_return_item item "
            + "INNER JOIN lunaerp_business_sales_return r ON item.return_id = r.id "
            + "WHERE item.deleted = 0 AND r.deleted = 0 "
            + "AND r.tenant_id = #{tenantId} "
            + "AND item.shipment_item_id IN "
            + "<foreach collection='shipmentItemIds' item='id' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach> "
            + "AND r.status &lt;&gt; 'CANCELLED' "
            + "AND r.approval_status IN ('APPROVING', 'APPROVED') "
            + "<if test='excludeReturnId != null'>"
            + "AND r.id &lt;&gt; #{excludeReturnId} "
            + "</if>"
            + "GROUP BY item.shipment_item_id"
            + "</script>")
    List<SalesReturnOccupiedQtyDTO> selectOccupiedQtyByShipmentItemIds(
            @Param("tenantId") Long tenantId,
            @Param("shipmentItemIds") Collection<Long> shipmentItemIds,
            @Param("excludeReturnId") Long excludeReturnId);

    /**
     * 查询已删除销售退货单下的明细（用于回收站恢复前引用校验）
     */
    List<SalesReturnItemDO> selectDeletedListByReturnId(@Param("returnId") Long returnId, @Param("tenantId") Long tenantId);

    /**
     * 级联恢复销售退货明细（显式 SET deleted=0 WHERE return_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByReturnId(@Param("returnId") Long returnId, @Param("tenantId") Long tenantId,
                          @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除销售退货明细（显式 DELETE WHERE return_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByReturnId(@Param("returnId") Long returnId, @Param("tenantId") Long tenantId);

}

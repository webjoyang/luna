package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 财务管理 - 发票申请明细行 Mapper
 *
 * @author luna
 */
@Mapper
public interface InvoiceApplicationItemMapper extends BaseMapperX<InvoiceApplicationItemDO> {

    /**
     * 按申请ID查询明细行列表
     */
    default List<InvoiceApplicationItemDO> selectListByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                .eq(InvoiceApplicationItemDO::getApplicationId, applicationId)
                .orderByAsc(InvoiceApplicationItemDO::getLineNo)
                .orderByAsc(InvoiceApplicationItemDO::getId));
    }

    /**
     * 软删除申请关联的所有明细行
     */
    @Update("UPDATE lunaerp_finance_invoice_application_item SET deleted = 1 "
            + "WHERE application_id = #{applicationId} AND deleted = 0")
    int deleteByApplicationId(@Param("applicationId") Long applicationId);

    /**
     * 查询指定发货单已被 SUBMITTED/ISSUED 发票占用的开票金额合计
     *
     * @param shipmentId 发货单ID
     * @param excludeApplicationId 排除的发票申请ID（修改/重提时传当前单ID，创建时传 null）
     * @return 已占用的开票金额（元），若无则返回 0
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(i.amount), 0) "
            + "FROM lunaerp_finance_invoice_application_item i "
            + "JOIN lunaerp_finance_invoice_application a ON a.id = i.application_id "
            + "WHERE i.shipment_id = #{shipmentId} "
            + "  AND i.deleted = 0 "
            + "  AND a.deleted = 0 "
            + "  AND a.status IN ('SUBMITTED', 'ISSUED') "
            + "<if test='excludeApplicationId != null'>"
            + "  AND a.id != #{excludeApplicationId} "
            + "</if>"
            + "</script>")
    BigDecimal selectInvoicedAmountByShipmentId(@Param("shipmentId") Long shipmentId,
                                                @Param("excludeApplicationId") Long excludeApplicationId);

    /**
     * 批量查询发货单已被 SUBMITTED/ISSUED 发票占用的开票金额合计
     */
    @Select("<script>"
            + "SELECT i.shipment_id AS shipmentId, COALESCE(SUM(i.amount), 0) AS invoicedAmount "
            + "FROM lunaerp_finance_invoice_application_item i "
            + "JOIN lunaerp_finance_invoice_application a ON a.id = i.application_id "
            + "WHERE i.shipment_id IN "
            + "<foreach collection='shipmentIds' item='item' open='(' separator=',' close=')'>"
            + "  #{item}"
            + "</foreach>"
            + "  AND i.deleted = 0 "
            + "  AND a.deleted = 0 "
            + "  AND a.status IN ('SUBMITTED', 'ISSUED') "
            + "GROUP BY i.shipment_id"
            + "</script>")
    List<Map<String, Object>> selectInvoicedAmountsByShipmentIds(@Param("shipmentIds") Collection<Long> shipmentIds);

    /**
     * 查询已删除发票申请下的明细行
     */
    @Select("SELECT * FROM lunaerp_finance_invoice_application_item "
            + "WHERE application_id = #{applicationId} AND deleted = 1 AND tenant_id = #{tenantId} "
            + "ORDER BY id ASC")
    List<InvoiceApplicationItemDO> selectDeletedListByApplicationId(@Param("applicationId") Long applicationId,
                                                                    @Param("tenantId") Long tenantId);

    /**
     * 级联恢复发票申请明细行
     */
    @Update("UPDATE lunaerp_finance_invoice_application_item "
            + "SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE application_id = #{applicationId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int restoreByApplicationId(@Param("applicationId") Long applicationId,
                               @Param("tenantId") Long tenantId,
                               @Param("updater") String updater,
                               @Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 彻底物理删除发票申请明细行
     */
    @Update("DELETE FROM lunaerp_finance_invoice_application_item "
            + "WHERE application_id = #{applicationId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int purgeByApplicationId(@Param("applicationId") Long applicationId,
                             @Param("tenantId") Long tenantId);

}

package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 销售退货防超退与可用量核算服务（唯一权威口径，ADR-0042）
 *
 * @author luna
 */
public interface SalesReturnAvailabilityService {

    /**
     * 批量查询发货明细行的已占用退货量
     *
     * @param shipmentItemIds 发货明细ID集合
     * @param excludeReturnId 排除的退货单ID（可空）
     * @return 发货明细ID -> 已占用退货量 Map
     */
    Map<Long, BigDecimal> getOccupiedQtyMap(Collection<Long> shipmentItemIds, Long excludeReturnId);

    /**
     * 查询单个发货明细行的已占用退货量
     *
     * @param shipmentItemId 发货明细ID
     * @param excludeReturnId 排除的退货单ID（可空）
     * @return 已占用退货量
     */
    BigDecimal getOccupiedQty(Long shipmentItemId, Long excludeReturnId);

    /**
     * 查询单个发货明细行的实时可退量（发货明细原发货量 - 累计占量）
     *
     * @param shipmentItemId 发货明细ID
     * @param excludeReturnId 排除的退货单ID（可空）
     * @return 实时可退量（>=0）
     */
    BigDecimal getAvailableQty(Long shipmentItemId, Long excludeReturnId);

    /**
     * 校验本次退货数量与金额是否超过可退上限（守卫 004/005/006）
     *
     * @param shipmentItemId 发货明细ID
     * @param returnQty 本次退货数量
     * @param unitPrice 本次退货单价（元，可改折让价；为空缺省原发货单价）
     * @param excludeReturnId 排除的退货单ID（修改时传入当前退货单ID）
     */
    void checkAvailability(Long shipmentItemId, BigDecimal returnQty, BigDecimal unitPrice, Long excludeReturnId);

}

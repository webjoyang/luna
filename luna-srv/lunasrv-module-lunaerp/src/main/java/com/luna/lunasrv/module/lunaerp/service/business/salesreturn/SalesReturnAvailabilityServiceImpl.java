package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.dto.SalesReturnOccupiedQtyDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.*;

/**
 * 销售退货防超退与可用量核算实现（唯一权威口径，ADR-0042）
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SalesReturnAvailabilityServiceImpl implements SalesReturnAvailabilityService {

    @Resource
    private SalesReturnItemMapper salesReturnItemMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Override
    public Map<Long, BigDecimal> getOccupiedQtyMap(Collection<Long> shipmentItemIds, Long excludeReturnId) {
        if (CollUtil.isEmpty(shipmentItemIds)) {
            return Collections.emptyMap();
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<SalesReturnOccupiedQtyDTO> occupiedList = salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(
                tenantId, shipmentItemIds, excludeReturnId);
        if (CollUtil.isEmpty(occupiedList)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> resultMap = new HashMap<>(occupiedList.size());
        for (SalesReturnOccupiedQtyDTO dto : occupiedList) {
            resultMap.put(dto.getShipmentItemId(), dto.getOccupiedQty());
        }
        return resultMap;
    }

    @Override
    public BigDecimal getOccupiedQty(Long shipmentItemId, Long excludeReturnId) {
        if (shipmentItemId == null) {
            return BigDecimal.ZERO;
        }
        Map<Long, BigDecimal> map = getOccupiedQtyMap(Collections.singletonList(shipmentItemId), excludeReturnId);
        return map.getOrDefault(shipmentItemId, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getAvailableQty(Long shipmentItemId, Long excludeReturnId) {
        ShipmentItemDO shipmentItem = shipmentItemMapper.selectById(shipmentItemId);
        if (shipmentItem == null) {
            throw exception(SALES_RETURN_ITEM_LINEAGE_INVALID);
        }
        BigDecimal originalQty = shipmentItem.getQuantity() != null ? shipmentItem.getQuantity() : BigDecimal.ZERO;
        BigDecimal occupiedQty = getOccupiedQty(shipmentItemId, excludeReturnId);
        BigDecimal availableQty = originalQty.subtract(occupiedQty);
        return availableQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableQty;
    }

    @Override
    public void checkAvailability(Long shipmentItemId, BigDecimal returnQty, BigDecimal unitPrice, Long excludeReturnId) {
        ShipmentItemDO shipmentItem = shipmentItemMapper.selectById(shipmentItemId);
        if (shipmentItem == null) {
            throw exception(SALES_RETURN_ITEM_LINEAGE_INVALID);
        }

        BigDecimal originalQty = shipmentItem.getQuantity() != null ? shipmentItem.getQuantity() : BigDecimal.ZERO;
        BigDecimal originalUnitPrice = shipmentItem.getUnitPrice() != null ? shipmentItem.getUnitPrice() : BigDecimal.ZERO;

        BigDecimal occupiedQty = getOccupiedQty(shipmentItemId, excludeReturnId);
        BigDecimal availableQty = originalQty.subtract(occupiedQty);
        if (availableQty.compareTo(BigDecimal.ZERO) < 0) {
            availableQty = BigDecimal.ZERO;
        }

        // 1. 数量防超退守卫
        if (returnQty != null && returnQty.compareTo(availableQty) > 0) {
            throw exception(SALES_RETURN_QTY_EXCEED_AVAILABLE, returnQty, availableQty);
        }

        // 2. 金额防超退额守卫：行金额 ≤ 可退量 × 原发货单价（允许协议折让降价，不允许超额授信）
        BigDecimal effectiveUnitPrice = unitPrice != null ? unitPrice : originalUnitPrice;
        BigDecimal lineQty = returnQty != null ? returnQty : BigDecimal.ZERO;
        BigDecimal lineAmount = lineQty.multiply(effectiveUnitPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxAllowedAmount = availableQty.multiply(originalUnitPrice).setScale(2, RoundingMode.HALF_UP);
        if (lineAmount.compareTo(maxAllowedAmount) > 0) {
            throw exception(SALES_RETURN_AMOUNT_EXCEED_AVAILABLE);
        }
    }

}

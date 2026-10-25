package com.luna.lunasrv.module.lunaerp.service.business.shipment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 发货明细快照 DTO（business 业务域自有事件载荷）
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentItemSnapshotDTO {

    /**
     * 发货明细ID（lunaerp_business_shipment_item.id）
     */
    private Long shipmentItemId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码快照
     */
    private String productCode;

    /**
     * 产品名称快照
     */
    private String productName;

    /**
     * 发货数量
     */
    private BigDecimal quantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 明细行金额（数量 × 单价）
     */
    private BigDecimal amount;

    /**
     * 备注
     */
    private String remark;

}

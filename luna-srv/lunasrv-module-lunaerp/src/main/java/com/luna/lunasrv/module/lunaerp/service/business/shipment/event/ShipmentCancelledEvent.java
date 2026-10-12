package com.luna.lunasrv.module.lunaerp.service.business.shipment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发货单已发货红冲事件（应收红冲点）
 *
 * business 侧在已发货红冲事务内（COMPLETED -> CANCELLED）发布此事件；
 * business 严禁 import finance 类，由 finance 监听器消费写 AR_INC_REVERSAL 分录。
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCancelledEvent {

    /**
     * 发货单ID
     */
    private Long shipmentId;

    /**
     * 发货单号
     */
    private String shipmentNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 运费（可空）
     */
    private BigDecimal freightFee;

    /**
     * 明细行快照数组（含回指所需信息）
     */
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        /**
         * 发货单明细ID
         */
        private Long shipmentItemId;

        /**
         * 行金额
         */
        private BigDecimal amount;

        /**
         * 币种
         */
        private String currency;

    }

}

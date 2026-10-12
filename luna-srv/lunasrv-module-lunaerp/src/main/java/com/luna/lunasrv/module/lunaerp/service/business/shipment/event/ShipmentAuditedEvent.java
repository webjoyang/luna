package com.luna.lunasrv.module.lunaerp.service.business.shipment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发货单审核出库事件（应收成立点）
 *
 * business 侧在审核出库事务内（DRAFT -> COMPLETED）发布此事件；
 * business 严禁 import finance 类，由 finance 监听器消费写经营分录账。
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentAuditedEvent {

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
     * 币种（CNY/USD/EUR/HKD等）
     */
    private String currency;

    /**
     * 发货业务日期
     */
    private LocalDate shipmentDate;

    /**
     * 运费（独立于货物金额）
     */
    private BigDecimal freightFee;

    /**
     * 明细行快照数组
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
         * 产品名称
         */
        private String productName;

        /**
         * 发货数量
         */
        private BigDecimal quantity;

        /**
         * 计量单位
         */
        private String unit;

        /**
         * 单价
         */
        private BigDecimal unitPrice;

        /**
         * 行金额
         */
        private BigDecimal amount;

    }

}

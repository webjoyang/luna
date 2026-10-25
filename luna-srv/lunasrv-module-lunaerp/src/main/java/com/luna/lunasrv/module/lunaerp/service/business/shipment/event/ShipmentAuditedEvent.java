package com.luna.lunasrv.module.lunaerp.service.business.shipment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发货单审核出库完成事件（business 业务域发布，finance 域监听）
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
     * 客户名称快照
     */
    private String customerName;

    /**
     * 币种（复用字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD 等）
     */
    private String currency;

    /**
     * 发货日期
     */
    private LocalDate shipmentDate;

    /**
     * 运费（头级运费，独立于货物金额）
     */
    private BigDecimal freightFee;

    /**
     * 发货明细快照列表
     */
    private List<ShipmentItemSnapshotDTO> items;

}

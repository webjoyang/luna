package com.luna.lunasrv.module.lunaerp.service.business.shipment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发货单红冲完成事件（business 业务域发布，finance 域监听）
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
     * 币种
     */
    private String currency;

    /**
     * 取消/红冲日期
     */
    private LocalDate cancelDate;

    /**
     * 运费
     */
    private BigDecimal freightFee;

    /**
     * 发货明细快照列表（用于定位回指原分录）
     */
    private List<ShipmentItemSnapshotDTO> items;

}

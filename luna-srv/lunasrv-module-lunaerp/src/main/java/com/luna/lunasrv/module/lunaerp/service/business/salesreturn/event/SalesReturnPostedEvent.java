package com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售退货过账完成事件（business 业务域发布，finance 域监听）
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnPostedEvent {

    /**
     * 退货单 ID
     */
    private Long returnId;

    /**
     * 退货单号
     */
    private String returnNo;

    /**
     * 原发货单 ID
     */
    private Long shipmentId;

    /**
     * 原发货单号快照
     */
    private String shipmentNo;

    /**
     * 客户 ID
     */
    private Long customerId;

    /**
     * 币种（复用字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD 等）
     */
    private String currency;

    /**
     * 退货日期
     */
    private LocalDate returnDate;

    /**
     * 逐行退货明细快照列表
     */
    private List<SalesReturnPostedItemDTO> items;

}

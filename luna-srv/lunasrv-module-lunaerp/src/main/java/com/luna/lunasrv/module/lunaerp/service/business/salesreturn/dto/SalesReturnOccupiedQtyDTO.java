package com.luna.lunasrv.module.lunaerp.service.business.salesreturn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售退货明细行占量汇总 DTO
 *
 * @author luna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnOccupiedQtyDTO {

    /**
     * 发货明细ID（lunaerp_business_shipment_item.id）
     */
    private Long shipmentItemId;

    /**
     * 已占用的退货数量汇总（Σ return_qty）
     */
    private BigDecimal occupiedQty;

}

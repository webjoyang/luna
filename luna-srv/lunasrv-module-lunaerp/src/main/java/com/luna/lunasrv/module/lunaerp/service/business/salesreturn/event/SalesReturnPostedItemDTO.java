package com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售退货过账逐行明细快照 DTO
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnPostedItemDTO {

    /**
     * 退货明细行 ID
     */
    private Long returnItemId;

    /**
     * 原发货明细行 ID
     */
    private Long shipmentItemId;

    /**
     * 退货计费数量
     */
    private BigDecimal returnQty;

    /**
     * 退货金额（原币）
     */
    private BigDecimal amount;

}

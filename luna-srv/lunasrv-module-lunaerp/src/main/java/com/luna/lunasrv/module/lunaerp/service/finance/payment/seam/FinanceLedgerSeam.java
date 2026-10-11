package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import java.math.BigDecimal;

/**
 * 经营分录账服务 Seam（10/12号分录账待落地件的外部接口防腐层）
 *
 * @author luna
 */
public interface FinanceLedgerSeam {

    /**
     * 追加供应商付款经营分录（OUT_SUPPLIER）
     *
     * @param paymentId 付款单ID
     * @param paymentNo 付款单号
     * @param supplierId 供应商ID
     * @param totalAmount 核销总金额
     * @param payAmount 本次实付金额
     * @param prepayUsedAmount 预付抵扣金额
     */
    void appendSupplierPaymentEntry(Long paymentId, String paymentNo, Long supplierId,
                                    BigDecimal totalAmount, BigDecimal payAmount, BigDecimal prepayUsedAmount);

}

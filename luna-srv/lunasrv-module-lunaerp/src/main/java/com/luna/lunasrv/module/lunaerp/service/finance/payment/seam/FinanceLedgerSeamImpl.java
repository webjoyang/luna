package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 经营分录账服务 Seam 默认实现（在 10/12 号分录账服务落地前提供桩支持）
 *
 * @author luna
 */
@Slf4j
@Service
public class FinanceLedgerSeamImpl implements FinanceLedgerSeam {

    @Override
    public void appendSupplierPaymentEntry(Long paymentId, String paymentNo, Long supplierId,
                                           BigDecimal totalAmount, BigDecimal payAmount, BigDecimal prepayUsedAmount) {
        log.info("[FinanceLedgerSeam] 追加供应商付款分录 OUT_SUPPLIER: paymentId={}, paymentNo={}, supplierId={}, "
                + "totalAmount={}, payAmount={}, prepayUsedAmount={}",
                paymentId, paymentNo, supplierId, totalAmount, payAmount, prepayUsedAmount);
    }

}

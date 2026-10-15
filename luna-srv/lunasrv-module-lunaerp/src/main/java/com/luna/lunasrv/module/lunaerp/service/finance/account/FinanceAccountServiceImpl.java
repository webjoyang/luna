package com.luna.lunasrv.module.lunaerp.service.finance.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 资金账户服务 Seam 默认占位实现（12 号未落地前的 Mock Seam）
 *
 * @author luna
 */
@Service
@Slf4j
public class FinanceAccountServiceImpl implements FinanceAccountService {

    @Override
    public void validateAccountAvailable(Long accountId) {
        log.info("[FinanceAccountService] validateAccountAvailable seam called: accountId={}", accountId);
    }

    @Override
    public void debitAccount(Long accountId, BigDecimal amount) {
        log.info("[FinanceAccountService] debitAccount seam called: accountId={}, amount={}", accountId, amount);
    }

    @Override
    public void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo) {
        log.info("[FinanceAccountService] recordAccountOutflow seam called: accountId={}, amount={}, paymentId={}, paymentNo={}",
                accountId, amount, paymentId, paymentNo);
    }

    @Override
    public void appendOutSupplierLedgerEntry(Long paymentId, String paymentNo, Long supplierId,
                                             BigDecimal totalAmount, BigDecimal prepayUsedAmount,
                                             Long accountId, String remark) {
        log.info("[FinanceAccountService] appendOutSupplierLedgerEntry seam called: paymentId={}, paymentNo={}, supplierId={}, totalAmount={}, prepayUsedAmount={}, accountId={}, remark={}",
                paymentId, paymentNo, supplierId, totalAmount, prepayUsedAmount, accountId, remark);
    }

}

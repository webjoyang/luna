package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金账户服务 Seam 实现类（已接入真实 AccountService 与 LedgerService）
 *
 * @author luna
 */
@Service
@Slf4j
public class FinanceAccountServiceImpl implements FinanceAccountService {

    @Resource
    private AccountService accountService;

    @Resource
    private LedgerService ledgerService;

    @Override
    public void validateAccountAvailable(Long accountId) {
        accountService.validateAccountAvailable(accountId);
    }

    @Override
    public void debitAccount(Long accountId, BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            accountService.updateBalance(accountId, amount.negate());
        }
    }

    @Override
    public void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo) {
        log.info("[FinanceAccountService] recordAccountOutflow: accountId={}, amount={}, paymentId={}, paymentNo={}",
                accountId, amount, paymentId, paymentNo);
    }

    @Override
    public void appendOutSupplierLedgerEntry(Long paymentId, String paymentNo, Long supplierId,
                                             BigDecimal totalAmount, BigDecimal prepayUsedAmount,
                                             Long accountId, String remark) {
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setBizDate(LocalDate.now());
        entry.setPartyType("SUPPLIER");
        entry.setPartyId(supplierId);
        entry.setAccountId(accountId);
        entry.setCategory("OUT_SUPPLIER");
        entry.setDirection("CREDIT");
        entry.setCurrency("RMB");
        entry.setExchangeRate(BigDecimal.ONE);
        entry.setOriginalAmount(totalAmount);
        entry.setBaseAmount(totalAmount);
        entry.setRefType("PAYMENT");
        entry.setRefId(paymentId);
        entry.setRemark(remark);
        ledgerService.append(entry);
    }

}

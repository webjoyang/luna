package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_ACCOUNT_NOT_AVAILABLE;

/**
 * 资金账户服务 Seam 实现（桥接至真实 AccountService）
 *
 * @author luna
 */
@Slf4j
@Service
public class FinanceAccountSeamImpl implements FinanceAccountSeam {

    @Resource
    private AccountService accountService;

    @Override
    public void deductAccountBalance(Long accountId, BigDecimal amount) {
        log.info("[FinanceAccountSeam] 资金账户扣减余额: accountId={}, amount={}", accountId, amount);
        if (accountId == null) {
            throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            accountService.updateBalance(accountId, amount.negate());
        }
    }

    @Override
    public void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo) {
        log.info("[FinanceAccountSeam] 记录资金账户流出流水: accountId={}, amount={}, paymentId={}, paymentNo={}",
                accountId, amount, paymentId, paymentNo);
    }

}

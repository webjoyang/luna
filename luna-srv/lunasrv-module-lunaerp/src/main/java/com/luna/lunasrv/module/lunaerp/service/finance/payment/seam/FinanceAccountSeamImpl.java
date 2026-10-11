package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_ACCOUNT_NOT_AVAILABLE;

/**
 * 资金账户服务 Seam 默认实现（在 12 号资金账户服务落地前提供桩支持）
 *
 * @author luna
 */
@Slf4j
@Service
public class FinanceAccountSeamImpl implements FinanceAccountSeam {

    @Override
    public void deductAccountBalance(Long accountId, BigDecimal amount) {
        log.info("[FinanceAccountSeam] 资金账户扣减余额: accountId={}, amount={}", accountId, amount);
        if (accountId == null) {
            throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
        }
        // 未接入真实账户表前的桩实现，默认放行；实际落地后校验账户状态与余额并执行条件更新
    }

    @Override
    public void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo) {
        log.info("[FinanceAccountSeam] 记录资金账户流出流水: accountId={}, amount={}, paymentId={}, paymentNo={}",
                accountId, amount, paymentId, paymentNo);
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import java.math.BigDecimal;

/**
 * 资金账户服务 Seam（12号待落地件的外部接口防腐层）
 *
 * @author luna
 */
public interface FinanceAccountSeam {

    /**
     * 校验资金账户状态并扣减余额
     *
     * @param accountId 资金账户ID
     * @param amount 扣减金额
     */
    void deductAccountBalance(Long accountId, BigDecimal amount);

    /**
     * 记录资金账户流出流水
     *
     * @param accountId 资金账户ID
     * @param amount 流出金额
     * @param paymentId 付款单ID
     * @param paymentNo 付款单号
     */
    void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo);

}

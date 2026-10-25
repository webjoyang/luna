package com.luna.lunasrv.module.lunaerp.service.finance.account;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户服务 Seam 接口（12 号落地件，未落地前以此 Mock Seam 表达）
 *
 * <p>冲突点挂账：
 * 1. 资金账户余额校验与条件扣减：调用 finance.account 服务条件更新扣减 pay_amount，余额不足报 023_004，账户停用报 023_006；
 * 2. 分录账 ledger 写入：付款登记写 OUT_SUPPLIER 分录，金额为 Σ(pay_amount + prepay_used_amount)，10/12 号共用 append-only 分录账；
 * 3. 待 12 号统一过账通道 FinancePostingService 落地后，本 seam 收口平滑切换。
 * </p>
 *
 * @author luna
 */
public interface FinanceAccountService {

    /**
     * 校验资金账户存在且未停用（若不存在或已停用抛 023_006 PAYMENT_ACCOUNT_NOT_AVAILABLE）
     *
     * @param accountId 资金账户ID
     */
    void validateAccountAvailable(Long accountId);

    /**
     * 条件扣减资金账户余额（pay_amount）
     * <p>若账户余额不足或条件更新失败抛 023_004 PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH</p>
     *
     * @param accountId 资金账户ID
     * @param amount 扣减金额
     */
    void debitAccount(Long accountId, BigDecimal amount);

    /**
     * 记录资金账户流出流水
     *
     * @param accountId 资金账户ID
     * @param amount 流出金额
     * @param paymentId 关联付款单ID
     * @param paymentNo 关联付款单号
     */
    void recordAccountOutflow(Long accountId, BigDecimal amount, Long paymentId, String paymentNo);

    /**
     * 追加经营分录（OUT_SUPPLIER）
     *
     * @param paymentId 关联付款单ID
     * @param paymentNo 关联付款单号
     * @param supplierId 供应商ID
     * @param totalAmount 分录总金额（实付 + 预付抵扣）
     * @param prepayUsedAmount 预付抵扣金额
     * @param accountId 资金账户ID
     * @param remark 备注（含 prepay_used_amount 信息）
     */
    void appendOutSupplierLedgerEntry(Long paymentId, String paymentNo, Long supplierId,
                                      BigDecimal totalAmount, BigDecimal prepayUsedAmount,
                                      Long accountId, String remark);

}

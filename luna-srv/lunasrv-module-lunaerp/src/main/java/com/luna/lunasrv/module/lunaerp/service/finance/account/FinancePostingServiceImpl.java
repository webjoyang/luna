package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 财务统一过账服务实现类（域公共唯一资金写入口，ADR-0043）
 *
 * <p>收口说明与挂账：
 * 1. 10 号收款 handler（ReceiptHandler）收口挂账：收款确认/过账时，统一注入本服务 post(PostingReq)，
 *    构造 direction=DEBIT, category=IN_CUSTOMER 进行条件更新与分录写入，语义零变化平滑收口；
 * 2. 11 号既有付款登记：既有 mock seam (FinanceAccountService) 抛出 023_004 / 020_101 契约保持完全兼容；
 * 3. 13 号销售退货往来分录：通过 {@link #appendArEntry(PostingReq)} 只读追加往来分录，account_id 为空不碰资金余额。
 * </p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class FinancePostingServiceImpl implements FinancePostingService {

    public static final String DIRECTION_DEBIT = "DEBIT";
    public static final String DIRECTION_CREDIT = "CREDIT";
    public static final String CURRENCY_RMB = "RMB";

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long post(PostingReq req) {
        if (req == null || req.getAccountId() == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }

        // 1. 查验资金账户：存在性 020_100、启用态 020_101、币种一致性 020_106
        AccountDO account = accountMapper.selectById(req.getAccountId());
        if (account == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }
        if (!AccountServiceImpl.STATUS_ENABLE.equals(account.getStatus())) {
            log.warn("[FinancePostingService] 资金账户已停用: accountId={}, status={}", account.getId(), account.getStatus());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }
        if (StrUtil.isBlank(req.getCurrency()) || !account.getCurrency().equalsIgnoreCase(req.getCurrency().trim())) {
            log.warn("[FinancePostingService] 币种不一致或为空: accountId={}, accountCurrency={}, reqCurrency={}",
                    account.getId(), account.getCurrency(), req.getCurrency());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH);
        }

        BigDecimal originalAmount = req.getOriginalAmount();
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(FinanceErrorCodeConstants.PAYMENT_AMOUNT_INVALID);
        }

        String direction = req.getDirection();
        if (StrUtil.isBlank(direction)) {
            throw invalidParamException("收支方向不能为空");
        }

        // 2. 条件更新 current_balance
        int updateCount;
        if (DIRECTION_DEBIT.equalsIgnoreCase(direction)) {
            // 流入：current_balance + amount
            updateCount = accountMapper.updateBalanceIncrease(req.getAccountId(), originalAmount);
        } else if (DIRECTION_CREDIT.equalsIgnoreCase(direction)) {
            // 流出：current_balance - amount（条件 current_balance >= amount）
            updateCount = accountMapper.updateBalanceDecrease(req.getAccountId(), originalAmount.negate(), originalAmount);
        } else {
            throw invalidParamException("非法收支方向: " + direction);
        }

        // updateCount=0：区分停用 020_101 或余额不足 023_004
        if (updateCount == 0) {
            AccountDO latest = accountMapper.selectById(req.getAccountId());
            if (latest == null) {
                throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
            }
            if (!AccountServiceImpl.STATUS_ENABLE.equals(latest.getStatus())) {
                log.warn("[FinancePostingService] 账户停用导致过账失败: accountId={}, status={}", latest.getId(), latest.getStatus());
                throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
            }
            if (DIRECTION_CREDIT.equalsIgnoreCase(direction)) {
                log.warn("[FinancePostingService] 账户余额不足导致流出过账失败: accountId={}, currentBalance={}, required={}",
                    latest.getId(), latest.getCurrentBalance(), originalAmount);
                throw exception(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH);
            }
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }

        // 3. 同事务 INSERT 经营分录
        LedgerEntryDO entry = buildLedgerEntry(req, req.getAccountId(), direction, account.getCurrency());
        Long entryId = ledgerService.append(entry);
        log.info("[FinancePostingService] 资金过账成功: entryId={}, accountId={}, direction={}, category={}, amount={}, baseAmount={}",
                entryId, req.getAccountId(), direction, req.getCategory(), originalAmount, entry.getBaseAmount());
        return entryId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long appendArEntry(PostingReq req) {
        if (req == null) {
            throw invalidParamException("往来分录请求不能为空");
        }

        String currency = StrUtil.isNotBlank(req.getCurrency()) ? req.getCurrency() : CURRENCY_RMB;
        // 13 号往来分录 account_id 恒为空，不触碰资金账户
        LedgerEntryDO entry = buildLedgerEntry(req, null, req.getDirection(), currency);
        Long entryId = ledgerService.append(entry);
        log.info("[FinancePostingService] 往来分录追加成功: entryId={}, category={}, direction={}, amount={}, baseAmount={}",
                entryId, req.getCategory(), entry.getDirection(), entry.getOriginalAmount(), entry.getBaseAmount());
        return entryId;
    }

    private LedgerEntryDO buildLedgerEntry(PostingReq req, Long accountId, String direction, String currency) {
        BigDecimal originalAmount = req.getOriginalAmount() != null ? req.getOriginalAmount() : BigDecimal.ZERO;
        BigDecimal rate = req.getExchangeRate();
        BigDecimal baseAmount;

        if (CURRENCY_RMB.equalsIgnoreCase(currency)) {
            rate = BigDecimal.ONE;
            baseAmount = originalAmount.setScale(2, RoundingMode.HALF_UP);
        } else {
            if (rate == null) {
                rate = BigDecimal.ONE;
            }
            baseAmount = originalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setBizDate(req.getBizDate() != null ? req.getBizDate() : LocalDate.now());
        entry.setPartyType(req.getPartyType());
        entry.setPartyId(req.getPartyId());
        entry.setPartyName(req.getPartyName());
        entry.setAccountId(accountId);
        entry.setCategory(req.getCategory());
        entry.setDirection(direction != null ? direction.toUpperCase() : null);
        entry.setCurrency(currency);
        entry.setExchangeRate(rate);
        entry.setOriginalAmount(originalAmount);
        entry.setBaseAmount(baseAmount);
        entry.setRefType(req.getRefType());
        entry.setRefId(req.getRefId());
        entry.setRefItemId(req.getRefItemId());
        entry.setRefNo(req.getRefNo());
        entry.setRemark(req.getRemark());
        return entry;
    }

}

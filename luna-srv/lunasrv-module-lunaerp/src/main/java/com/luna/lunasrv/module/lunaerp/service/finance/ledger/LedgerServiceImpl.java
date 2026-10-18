package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 财务管理 - 经营分录账 Service 实现类（append-only 唯一账本，ADR-0043）
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long append(LedgerEntryDO entry) {
        if (entry == null) {
            throw invalidParamException("分录对象不能为空");
        }
        ledgerEntryMapper.insert(entry);
        log.info("[LedgerService] 成功追加经营分录: id={}, category={}, partyType={}, partyId={}, refType={}, refId={}, amount={}",
                entry.getId(), entry.getCategory(), entry.getPartyType(), entry.getPartyId(), entry.getRefType(), entry.getRefId(), entry.getOriginalAmount());
        return entry.getId();
    }

    /**
     * 应用层分录不可变守卫：试图修改分录抛出 1_030_020_000 LEDGER_IMMUTABLE
     */
    public void updateEntry(LedgerEntryDO entry) {
        log.warn("[LedgerService] 试图更新分录，被不可变守卫拒绝: entryId={}", entry != null ? entry.getId() : null);
        throw exception(FinanceErrorCodeConstants.LEDGER_IMMUTABLE);
    }

    /**
     * 应用层分录不可变守卫：试图删除分录抛出 1_030_020_000 LEDGER_IMMUTABLE
     */
    public void deleteEntry(Long id) {
        log.warn("[LedgerService] 试图删除分录，被不可变守卫拒绝: id={}", id);
        throw exception(FinanceErrorCodeConstants.LEDGER_IMMUTABLE);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByRef(String refType, Long refId) {
        if (StrUtil.isBlank(refType) || refId == null) {
            return Collections.emptyList();
        }
        return ledgerEntryMapper.selectListByRef(refType, refId);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByRefAndItemId(String refType, Long refId, Long refItemId) {
        if (StrUtil.isBlank(refType) || refId == null || refItemId == null) {
            return Collections.emptyList();
        }
        return ledgerEntryMapper.selectListByRefAndItemId(refType, refId, refItemId);
    }

    @Override
    public List<LedgerEntryDO> getReversalEntries(Long reversalOfId) {
        if (reversalOfId == null) {
            return Collections.emptyList();
        }
        return ledgerEntryMapper.selectListByReversalOfId(reversalOfId);
    }

    @Override
    public List<LedgerEntryDO> getReversalEntriesByReversalOfIds(Collection<Long> reversalOfIds) {
        if (CollUtil.isEmpty(reversalOfIds)) {
            return Collections.emptyList();
        }
        return ledgerEntryMapper.selectListByReversalOfIds(reversalOfIds);
    }

    @Override
    public Long countEntriesByAccountId(Long accountId) {
        if (accountId == null) {
            return 0L;
        }
        return ledgerEntryMapper.selectCountByAccountId(accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordArReturnReversal(Long customerId, Long shipmentItemId, BigDecimal returnQty,
                                        BigDecimal origAmount, String currency,
                                        BigDecimal exchangeRate, Long returnOrderId) {
        BigDecimal rate = exchangeRate != null ? exchangeRate : BigDecimal.ONE;
        BigDecimal baseAmount = origAmount != null
                ? origAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setBizDate(LocalDate.now());
        entry.setPartyType("CUSTOMER");
        entry.setPartyId(customerId);
        entry.setCategory(LedgerCategory.AR_RETURN_REVERSAL.getValue());
        entry.setDirection("CREDIT");
        entry.setCurrency(currency);
        entry.setExchangeRate(rate);
        entry.setOriginalAmount(origAmount);
        entry.setBaseAmount(baseAmount);
        entry.setRefType("SALES_RETURN");
        entry.setRefId(returnOrderId);
        entry.setRefItemId(shipmentItemId);
        entry.setRemark("销售退货应收冲减");

        return append(entry);
    }

}


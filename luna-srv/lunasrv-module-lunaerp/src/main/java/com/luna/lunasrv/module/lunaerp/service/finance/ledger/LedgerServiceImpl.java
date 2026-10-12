package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.LEDGER_IMMUTABLE;

/**
 * 财务管理 - 经营分录账服务实现类（域公共 append-only）
 *
 * 核心守卫：
 * 1. append-only：唯一写入通道为 append；
 * 2. 禁改禁删：update/delete 调用一律抛 1_030_020_000 LEDGER_IMMUTABLE；
 * 3. 自动计算本位币金额（原币 * 汇率，保留两位小数四舍五入）。
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class LedgerServiceImpl implements LedgerService {

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long append(LedgerEntryDO entry) {
        if (entry == null) {
            throw exception(LEDGER_IMMUTABLE);
        }
        // 校验分录类别
        if (StrUtil.isBlank(entry.getCategory()) || !FinanceLedgerCategoryEnum.isValid(entry.getCategory())) {
            log.warn("[append] 分录类别非法或为空: {}", entry.getCategory());
        }

        // 默认业务日期
        if (entry.getBizDate() == null) {
            entry.setBizDate(LocalDate.now());
        }

        // 默认汇率
        if (entry.getExchangeRate() == null || entry.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            entry.setExchangeRate(BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY));
        }

        // 默认原币金额
        if (entry.getOriginalAmount() == null) {
            entry.setOriginalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
        }

        // 计算本位币金额
        if (entry.getBaseAmount() == null) {
            BigDecimal base = entry.getOriginalAmount().multiply(entry.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
            entry.setBaseAmount(base);
        }

        // 插入分录
        ledgerEntryMapper.insert(entry);
        log.info("[append] 追加经营分录成功: id={}, category={}, partyType={}, partyId={}, originalAmount={}, baseAmount={}",
                entry.getId(), entry.getCategory(), entry.getPartyType(), entry.getPartyId(),
                entry.getOriginalAmount(), entry.getBaseAmount());
        return entry.getId();
    }

    @Override
    public void update(LedgerEntryDO entry) {
        log.error("[update] 经营分录账禁止修改: entry={}", entry);
        throw exception(LEDGER_IMMUTABLE);
    }

    @Override
    public void delete(Long id) {
        log.error("[delete] 经营分录账禁止删除: id={}", id);
        throw exception(LEDGER_IMMUTABLE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordArReturnReversal(Long customerId, Long shipmentItemId, BigDecimal returnQty,
                                        BigDecimal origAmount, String currency, BigDecimal exchangeRate,
                                        Long returnOrderId) {
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            exchangeRate = BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY);
        }
        BigDecimal baseAmount = origAmount != null
                ? origAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .bizDate(LocalDate.now())
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(customerId)
                .category(FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue())
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .currency(currency != null ? currency : "CNY")
                .exchangeRate(exchangeRate)
                .originalAmount(origAmount != null ? origAmount : BigDecimal.ZERO)
                .baseAmount(baseAmount)
                .refType(FinanceLedgerRefTypeEnum.RETURN_ORDER.getValue())
                .refId(returnOrderId)
                .refItemId(shipmentItemId)
                .remark("销售退货冲减应收，退货单ID=" + returnOrderId + "，退货数量=" + returnQty)
                .build();

        return append(entry);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByParty(String partyType, Long partyId, LocalDate startDate, LocalDate endDate) {
        return ledgerEntryMapper.selectListByParty(partyType, partyId, startDate, endDate);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByRef(String refType, Long refId) {
        return ledgerEntryMapper.selectListByRef(refType, refId);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByRefId(Long refId) {
        return ledgerEntryMapper.selectListByRefId(refId);
    }

    @Override
    public LedgerEntryDO getEntryById(Long id) {
        return ledgerEntryMapper.selectById(id);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByReversalOfId(Long reversalOfId) {
        return ledgerEntryMapper.selectListByReversalOfId(reversalOfId);
    }

    @Override
    public List<LedgerEntryDO> getEntriesByAccount(Long accountId, LocalDate startDate, LocalDate endDate) {
        return ledgerEntryMapper.selectListByAccount(accountId, startDate, endDate);
    }

    @Override
    public long countEntriesByAccountId(Long accountId) {
        if (accountId == null) {
            return 0L;
        }
        Long count = ledgerEntryMapper.selectCountByAccountId(accountId);
        return count != null ? count : 0L;
    }

}

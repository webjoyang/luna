package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerCustomerBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private ExchangeRateService exchangeRateService;

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

    @Override
    public PageResult<LedgerRespVO> getLedgerPage(LedgerPageReqVO reqVO) {
        PageResult<LedgerEntryDO> pageResult = ledgerEntryMapper.selectPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询客户名称
        Set<Long> customerIds = pageResult.getList().stream()
                .filter(e -> FinancePartyTypeEnum.CUSTOMER.getValue().equals(e.getPartyType()) && e.getPartyId() != null)
                .map(LedgerEntryDO::getPartyId)
                .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(customerIds)) {
            List<CustomerDO> customers = customerMapper.selectBatchIds(customerIds);
            customerNameMap = customers.stream()
                    .filter(c -> c.getCompanyName() != null)
                    .collect(Collectors.toMap(CustomerDO::getId, CustomerDO::getCompanyName, (a, b) -> a));
        }

        // 批量查询账户名称
        Set<Long> accountIds = pageResult.getList().stream()
                .map(LedgerEntryDO::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> accountNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(accountIds)) {
            List<AccountDO> accounts = accountMapper.selectBatchIds(accountIds);
            accountNameMap = accounts.stream()
                    .filter(a -> a.getAccountName() != null)
                    .collect(Collectors.toMap(AccountDO::getId, AccountDO::getAccountName, (a, b) -> a));
        }

        Map<Long, String> finalCustomerNameMap = customerNameMap;
        Map<Long, String> finalAccountNameMap = accountNameMap;
        List<LedgerRespVO> respList = pageResult.getList().stream().map(entry -> {
            String partyName = FinancePartyTypeEnum.CUSTOMER.getValue().equals(entry.getPartyType())
                    ? finalCustomerNameMap.get(entry.getPartyId())
                    : null;
            String accountName = entry.getAccountId() != null ? finalAccountNameMap.get(entry.getAccountId()) : null;
            return LedgerRespVO.builder()
                    .id(entry.getId())
                    .bizDate(entry.getBizDate())
                    .partyType(entry.getPartyType())
                    .partyId(entry.getPartyId())
                    .partyName(partyName)
                    .accountId(entry.getAccountId())
                    .accountName(accountName)
                    .category(entry.getCategory())
                    .direction(entry.getDirection())
                    .currency(entry.getCurrency())
                    .exchangeRate(entry.getExchangeRate())
                    .originalAmount(entry.getOriginalAmount())
                    .baseAmount(entry.getBaseAmount())
                    .refType(entry.getRefType())
                    .refId(entry.getRefId())
                    .refItemId(entry.getRefItemId())
                    .reversalOfId(entry.getReversalOfId())
                    .remark(entry.getRemark())
                    .createTime(entry.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<LedgerCustomerBalanceRespVO> getCustomerBalanceList(Long customerId, String currency) {
        // 查询该租户下客户维度的分录
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                .eqIfPresent(LedgerEntryDO::getPartyId, customerId)
                .eqIfPresent(LedgerEntryDO::getCurrency, currency)
                .orderByAsc(LedgerEntryDO::getPartyId)
                .orderByAsc(LedgerEntryDO::getCurrency));

        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }

        // 获取客户名称
        Set<Long> partyIds = entries.stream().map(LedgerEntryDO::getPartyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(partyIds)) {
            List<CustomerDO> customers = customerMapper.selectBatchIds(partyIds);
            customerNameMap = customers.stream()
                    .filter(c -> c.getCompanyName() != null)
                    .collect(Collectors.toMap(CustomerDO::getId, CustomerDO::getCompanyName, (a, b) -> a));
        }

        // 按 (partyId, currency) 分组聚合
        Map<String, List<LedgerEntryDO>> grouped = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getPartyId() + ":" + (StrUtil.isNotBlank(e.getCurrency()) ? e.getCurrency() : "CNY")));

        List<LedgerCustomerBalanceRespVO> resultList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (List<LedgerEntryDO> groupEntries : grouped.values()) {
            if (groupEntries.isEmpty()) {
                continue;
            }
            LedgerEntryDO first = groupEntries.get(0);
            Long cid = first.getPartyId();
            String curr = StrUtil.isNotBlank(first.getCurrency()) ? first.getCurrency() : "CNY";

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal historicalBaseBalance = BigDecimal.ZERO;

            // AR 应收相关借方类别（增加应收余额）
            Set<String> arDebitCategories = Set.of(
                    FinanceLedgerCategoryEnum.AR_INC.getValue());
            // AR 应收相关贷方类别（减少应收余额），RECEIPT/ADVANCE 现金分录不计入 AR 余额
            Set<String> arCreditCategories = Set.of(
                    FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue(),
                    FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue(),
                    FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue(),
                    FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue());

            for (LedgerEntryDO e : groupEntries) {
                BigDecimal orig = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                BigDecimal base = e.getBaseAmount() != null ? e.getBaseAmount() : BigDecimal.ZERO;
                String cat = e.getCategory();
                if (cat != null && arDebitCategories.contains(cat)
                        && FinanceLedgerDirectionEnum.DEBIT.getValue().equalsIgnoreCase(e.getDirection())) {
                    totalDebit = totalDebit.add(orig);
                    historicalBaseBalance = historicalBaseBalance.add(base);
                } else if (cat != null && arCreditCategories.contains(cat)
                        && FinanceLedgerDirectionEnum.CREDIT.getValue().equalsIgnoreCase(e.getDirection())) {
                    totalCredit = totalCredit.add(orig);
                    historicalBaseBalance = historicalBaseBalance.subtract(base);
                }
            }

            BigDecimal balance = totalDebit.subtract(totalCredit);

            // 获取当前即期汇率
            BigDecimal currentRate = BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY);
            if (isNotBaseCurrency(curr)) {
                try {
                    currentRate = exchangeRateService.getRate(curr, today);
                } catch (Exception ex) {
                    log.warn("[getCustomerBalanceList] 获取当前汇率异常，降级处理: currency={}, error={}", curr, ex.getMessage());
                }
            }

            BigDecimal currentBaseBalance = balance.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal exchangeDiff = currentBaseBalance.subtract(historicalBaseBalance);

            resultList.add(LedgerCustomerBalanceRespVO.builder()
                    .customerId(cid)
                    .customerName(customerNameMap.get(cid))
                    .currency(curr)
                    .totalDebit(totalDebit.setScale(2, RoundingMode.HALF_UP))
                    .totalCredit(totalCredit.setScale(2, RoundingMode.HALF_UP))
                    .balance(balance.setScale(2, RoundingMode.HALF_UP))
                    .historicalBaseBalance(historicalBaseBalance.setScale(2, RoundingMode.HALF_UP))
                    .currentRate(currentRate)
                    .currentBaseBalance(currentBaseBalance)
                    .exchangeDiff(exchangeDiff.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        // 按 customerId, currency 排序
        resultList.sort(Comparator.comparing(LedgerCustomerBalanceRespVO::getCustomerId)
                .thenComparing(LedgerCustomerBalanceRespVO::getCurrency));

        return resultList;
    }

    private boolean isNotBaseCurrency(String currency) {
        return StrUtil.isNotBlank(currency)
                && !"CNY".equalsIgnoreCase(currency)
                && !"RMB".equalsIgnoreCase(currency);
    }

}

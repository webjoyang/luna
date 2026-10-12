package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 客户对账单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class CustomerStatementServiceImpl implements CustomerStatementService {

    private static final String STATEMENT_NO_PREFIX = "CS";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final int NO_MIN_DIGITS = 4;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final int MAX_GENERATE_RETRY = 5;

    @Resource
    private CustomerStatementMapper customerStatementMapper;

    @Resource
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource(name = "lunaerpShipmentMapper")
    private ShipmentMapper shipmentMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Override
    public PageResult<CustomerStatementRespVO> getStatementPage(CustomerStatementPageReqVO pageReqVO) {
        LambdaQueryWrapperX<CustomerStatementDO> wrapper = new LambdaQueryWrapperX<CustomerStatementDO>()
                .likeIfPresent(CustomerStatementDO::getStatementNo, pageReqVO.getStatementNo())
                .eqIfPresent(CustomerStatementDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(CustomerStatementDO::getCustomerName, pageReqVO.getCustomerName())
                .eqIfPresent(CustomerStatementDO::getCurrency, pageReqVO.getCurrency())
                .eqIfPresent(CustomerStatementDO::getStatus, pageReqVO.getStatus())
                .geIfPresent(CustomerStatementDO::getPeriodStart, pageReqVO.getPeriodStart())
                .leIfPresent(CustomerStatementDO::getPeriodEnd, pageReqVO.getPeriodEnd())
                .orderByDesc(CustomerStatementDO::getPeriodEnd)
                .orderByDesc(CustomerStatementDO::getId);

        PageResult<CustomerStatementDO> pageResult = customerStatementMapper.selectPage(pageReqVO, wrapper);
        List<CustomerStatementRespVO> voList = pageResult.getList().stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public CustomerStatementDetailRespVO getStatement(Long id) {
        CustomerStatementDO statement = validateStatementExists(id);
        List<CustomerStatementItemDO> items = customerStatementItemMapper.selectListByStatementId(id);

        CustomerStatementDetailRespVO detailVO = new CustomerStatementDetailRespVO();
        copyPropertiesToRespVO(statement, detailVO);
        detailVO.setItems(items.stream().map(this::convertItemToRespVO).collect(Collectors.toList()));
        return detailVO;
    }

    @Override
    public CustomerStatementPrefetchRespVO prefetchLines(CustomerStatementPrefetchReqVO reqVO) {
        validatePeriod(reqVO.getPeriodStart(), reqVO.getPeriodEnd());
        CustomerDO customer = validateCustomer(reqVO.getCustomerId());

        String currency = reqVO.getCurrency().trim();
        LocalDate periodStart = reqVO.getPeriodStart();
        LocalDate periodEnd = reqVO.getPeriodEnd();
        Long excludeStatementId = reqVO.getExcludeStatementId();

        // 1. 计算期初余额（起始日前未核销 AR 余额）
        BigDecimal openingBalance = calculateOpeningBalance(customer.getId(), currency, periodStart);

        // 2. 检索区间内候选分录
        List<CustomerStatementCandidateItemVO> candidateItems = fetchCandidateItems(
                customer.getId(), currency, periodStart, periodEnd, excludeStatementId);

        // 3. 试算汇总
        BigDecimal candidateDebit = BigDecimal.ZERO;
        BigDecimal candidateCredit = BigDecimal.ZERO;
        for (CustomerStatementCandidateItemVO item : candidateItems) {
            if (Boolean.TRUE.equals(item.getOccupied())) {
                continue; // 试算不纳入已占用行
            }
            if (item.getDebitAmount() != null) {
                candidateDebit = candidateDebit.add(item.getDebitAmount());
            }
            if (item.getCreditAmount() != null) {
                candidateCredit = candidateCredit.add(item.getCreditAmount());
            }
        }
        BigDecimal estimatedClosingBalance = openingBalance.add(candidateDebit).subtract(candidateCredit);

        return CustomerStatementPrefetchRespVO.builder()
                .customerId(customer.getId())
                .customerName(customer.getCompanyName())
                .currency(currency)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingBalance(openingBalance)
                .candidateDebit(candidateDebit)
                .candidateCredit(candidateCredit)
                .estimatedClosingBalance(estimatedClosingBalance)
                .items(candidateItems)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStatement(CustomerStatementCreateReqVO reqVO) {
        validatePeriod(reqVO.getPeriodStart(), reqVO.getPeriodEnd());
        CustomerDO customer = validateCustomer(reqVO.getCustomerId());
        String currency = reqVO.getCurrency().trim();
        LocalDate periodStart = reqVO.getPeriodStart();
        LocalDate periodEnd = reqVO.getPeriodEnd();

        // 1. 生成或校验单号
        String statementNo;
        if (StringUtils.hasText(reqVO.getStatementNo())) {
            statementNo = reqVO.getStatementNo().trim();
            CustomerStatementDO existing = customerStatementMapper.selectByStatementNoIncludingDeleted(statementNo);
            if (existing != null) {
                throw exception(BAD_REQUEST, "对账单号【" + statementNo + "】已存在");
            }
        } else {
            statementNo = generateStatementNoWithRetry();
        }

        // 2. 计算期初余额（起始日前未核销 AR 余额）
        BigDecimal openingBalance = calculateOpeningBalance(customer.getId(), currency, periodStart);

        // 3. 确定纳入对账的分录项
        List<CustomerStatementCandidateItemVO> selectedCandidates;
        List<Long> occupiedIds = customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds();
        Set<Long> occupiedIdSet = new HashSet<>(occupiedIds);

        if (CollUtil.isNotEmpty(reqVO.getSelectedEntryIds())) {
            selectedCandidates = new ArrayList<>();
            for (Long entryId : reqVO.getSelectedEntryIds()) {
                LedgerEntryDO entry = ledgerEntryMapper.selectById(entryId);
                if (entry == null) {
                    continue;
                }
                // 校验分录客户与对账单客户一致
                if (!FinancePartyTypeEnum.CUSTOMER.getValue().equalsIgnoreCase(entry.getPartyType())
                        || !customer.getId().equals(entry.getPartyId())) {
                    throw exception(STATEMENT_CUSTOMER_MISMATCH);
                }
                // 校验币种一致
                if (!currency.equalsIgnoreCase(entry.getCurrency())) {
                    throw exception(STATEMENT_CURRENCY_MIXED);
                }
                // 校验行占用
                if (occupiedIdSet.contains(entry.getId())) {
                    throw exception(STATEMENT_ENTRY_OCCUPIED);
                }
                CustomerStatementCandidateItemVO candidate = buildCandidateFromEntry(entry, null);
                if (candidate != null) {
                    selectedCandidates.add(candidate);
                }
            }
        } else {
            // 未指定时默认包含区间内所有未被占用的候选分录
            List<CustomerStatementCandidateItemVO> allCandidates = fetchCandidateItems(
                    customer.getId(), currency, periodStart, periodEnd, null);
            selectedCandidates = allCandidates.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getOccupied()))
                    .collect(Collectors.toList());
        }

        // 守卫：期间内无可对账分录报错
        if (CollUtil.isEmpty(selectedCandidates)) {
            throw exception(STATEMENT_NO_PERIOD_ENTRIES);
        }

        // 4. 计算借方发生额、贷方发生额、期末余额
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        for (CustomerStatementCandidateItemVO c : selectedCandidates) {
            if (c.getDebitAmount() != null) {
                periodDebit = periodDebit.add(c.getDebitAmount());
            }
            if (c.getCreditAmount() != null) {
                periodCredit = periodCredit.add(c.getCreditAmount());
            }
        }
        BigDecimal closingBalance = openingBalance.add(periodDebit).subtract(periodCredit);

        // 4. 保存主表（DRAFT）
        CustomerStatementDO statement = CustomerStatementDO.builder()
                .statementNo(statementNo)
                .customerId(customer.getId())
                .customerName(customer.getCompanyName())
                .currency(currency)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingBalance(openingBalance)
                .periodDebit(periodDebit)
                .periodCredit(periodCredit)
                .closingBalance(closingBalance)
                .status(CustomerStatementStatusEnum.DRAFT.getValue())
                .remark(reqVO.getRemark())
                .build();
        customerStatementMapper.insert(statement);

        // 5. 保存明细行（快照冻结）
        int lineNo = 1;
        for (CustomerStatementCandidateItemVO c : selectedCandidates) {
            CustomerStatementItemDO item = CustomerStatementItemDO.builder()
                    .statementId(statement.getId())
                    .lineNo(lineNo++)
                    .ledgerEntryId(c.getLedgerEntryId())
                    .direction(c.getDirection())
                    .bizDate(c.getBizDate())
                    .refType(c.getRefType())
                    .refNo(c.getRefNo())
                    .summary(c.getSummary())
                    .debitAmount(c.getDebitAmount() != null ? c.getDebitAmount() : BigDecimal.ZERO)
                    .creditAmount(c.getCreditAmount() != null ? c.getCreditAmount() : BigDecimal.ZERO)
                    .signStatus(c.getSignStatus())
                    .build();
            customerStatementItemMapper.insert(item);
        }

        log.info("[createStatement] 成功生成客户对账单草稿: id={}, statementNo={}, itemCount={}",
                statement.getId(), statement.getStatementNo(), selectedCandidates.size());
        return statement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatement(CustomerStatementUpdateReqVO reqVO) {
        CustomerStatementDO statement = validateStatementExists(reqVO.getId());
        if (!CustomerStatementStatusEnum.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
        statement.setRemark(reqVO.getRemark());
        customerStatementMapper.updateById(statement);
        log.info("[updateStatement] 更新客户对账单草稿备注成功: id={}", statement.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStatement(Long id) {
        CustomerStatementDO statement = validateStatementExists(id);
        if (!CustomerStatementStatusEnum.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        // 删除主表与子表（软删除，释放行占用）
        customerStatementMapper.deleteById(id);
        customerStatementItemMapper.delete(new LambdaQueryWrapper<CustomerStatementItemDO>()
                .eq(CustomerStatementItemDO::getStatementId, id));

        log.info("[deleteStatement] 删除客户对账单草稿成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEntry(CustomerStatementAddEntryReqVO reqVO) {
        CustomerStatementDO statement = validateStatementExists(reqVO.getStatementId());
        if (!CustomerStatementStatusEnum.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        LedgerEntryDO entry = ledgerEntryMapper.selectById(reqVO.getLedgerEntryId());
        if (entry == null) {
            throw exception(BAD_REQUEST, "指定分录不存在");
        }

        // 校验客户一致
        if (!FinancePartyTypeEnum.CUSTOMER.getValue().equalsIgnoreCase(entry.getPartyType())
                || !statement.getCustomerId().equals(entry.getPartyId())) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }

        // 校验币种一致
        if (!statement.getCurrency().trim().equalsIgnoreCase(entry.getCurrency())) {
            throw exception(STATEMENT_CURRENCY_MIXED);
        }

        // 校验分录未被其他对账单占用
        List<Long> occupiedIds = customerStatementItemMapper.selectActiveOccupiedLedgerEntryIdsExcluding(statement.getId());
        if (occupiedIds.contains(entry.getId())) {
            throw exception(STATEMENT_ENTRY_OCCUPIED);
        }

        // 校验当前对账单是否已包含该分录
        List<CustomerStatementItemDO> existingItems = customerStatementItemMapper.selectListByStatementId(statement.getId());
        boolean alreadyInCurrent = existingItems.stream()
                .anyMatch(i -> entry.getId().equals(i.getLedgerEntryId()));
        if (alreadyInCurrent) {
            throw exception(STATEMENT_ENTRY_OCCUPIED);
        }

        CustomerStatementCandidateItemVO candidate = buildCandidateFromEntry(entry, null);
        if (candidate == null) {
            throw exception(BAD_REQUEST, "该分录已被红冲冲销，无法追加");
        }

        int nextLineNo = existingItems.size() + 1;
        CustomerStatementItemDO newItem = CustomerStatementItemDO.builder()
                .statementId(statement.getId())
                .lineNo(nextLineNo)
                .ledgerEntryId(entry.getId())
                .direction(candidate.getDirection())
                .bizDate(candidate.getBizDate())
                .refType(candidate.getRefType())
                .refNo(candidate.getRefNo())
                .summary(candidate.getSummary())
                .debitAmount(candidate.getDebitAmount() != null ? candidate.getDebitAmount() : BigDecimal.ZERO)
                .creditAmount(candidate.getCreditAmount() != null ? candidate.getCreditAmount() : BigDecimal.ZERO)
                .signStatus(candidate.getSignStatus())
                .build();
        customerStatementItemMapper.insert(newItem);

        // 重算主表发生额与期末余额
        BigDecimal newDebit = statement.getPeriodDebit().add(newItem.getDebitAmount());
        BigDecimal newCredit = statement.getPeriodCredit().add(newItem.getCreditAmount());
        BigDecimal newClosing = statement.getOpeningBalance().add(newDebit).subtract(newCredit);
        statement.setPeriodDebit(newDebit);
        statement.setPeriodCredit(newCredit);
        statement.setClosingBalance(newClosing);
        customerStatementMapper.updateById(statement);

        log.info("[addEntry] 草稿对账单追加分录成功: statementId={}, ledgerEntryId={}, lineNo={}",
                statement.getId(), entry.getId(), nextLineNo);
    }

    // ==================== 私有辅助方法 ====================

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)) {
            throw exception(STATEMENT_PERIOD_INVALID);
        }
    }

    private CustomerDO validateCustomer(Long customerId) {
        if (customerId == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }
        return customer;
    }

    private CustomerStatementDO validateStatementExists(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        if (statement == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        return statement;
    }

    /**
     * 计算期初余额（起始日前未核销 AR 余额 = 起始日前借方发生额合计 - 起始日前贷方发生额合计）
     */
    private BigDecimal calculateOpeningBalance(Long customerId, String currency, LocalDate periodStart) {
        List<LedgerEntryDO> priorEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                .eq(LedgerEntryDO::getPartyId, customerId)
                .eq(LedgerEntryDO::getCurrency, currency)
                .lt(LedgerEntryDO::getBizDate, periodStart));

        if (CollUtil.isEmpty(priorEntries)) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal priorDebit = BigDecimal.ZERO;
        BigDecimal priorCredit = BigDecimal.ZERO;

        for (LedgerEntryDO entry : priorEntries) {
            BigDecimal amt = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            String cat = entry.getCategory();

            if (FinanceLedgerCategoryEnum.AR_INC.getValue().equals(cat)
                    || FinanceLedgerCategoryEnum.RECEIPT_REVERSAL.getValue().equals(cat)) {
                priorDebit = priorDebit.add(amt);
            } else if (FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue().equals(cat)
                    || FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue().equals(cat)
                    || FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue().equals(cat)
                    || FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue().equals(cat)) {
                priorCredit = priorCredit.add(amt);
            }
        }

        return priorDebit.subtract(priorCredit).setScale(2);
    }

    /**
     * 检索区间内候选分录列表（含占用标识）
     */
    private List<CustomerStatementCandidateItemVO> fetchCandidateItems(Long customerId, String currency,
                                                                       LocalDate periodStart, LocalDate periodEnd,
                                                                       Long excludeStatementId) {
        List<String> validCategories = List.of(
                FinanceLedgerCategoryEnum.AR_INC.getValue(),
                FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue(),
                FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue(),
                FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue()
        );

        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                .eq(LedgerEntryDO::getPartyId, customerId)
                .eq(LedgerEntryDO::getCurrency, currency)
                .ge(LedgerEntryDO::getBizDate, periodStart)
                .le(LedgerEntryDO::getBizDate, periodEnd)
                .in(LedgerEntryDO::getCategory, validCategories)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }

        // 查占用映射表
        List<Map<String, Object>> occupancyMapList = customerStatementItemMapper.selectActiveOccupiedMapList();
        Map<Long, Map<String, Object>> occupiedMap = new HashMap<>();
        if (CollUtil.isNotEmpty(occupancyMapList)) {
            for (Map<String, Object> map : occupancyMapList) {
                Object entryIdObj = map.get("ledgerEntryId");
                if (entryIdObj != null) {
                    Long entryId = Long.valueOf(entryIdObj.toString());
                    occupiedMap.put(entryId, map);
                }
            }
        }

        List<CustomerStatementCandidateItemVO> result = new ArrayList<>();
        for (LedgerEntryDO entry : entries) {
            Map<String, Object> occInfo = occupiedMap.get(entry.getId());
            CustomerStatementCandidateItemVO candidate = buildCandidateFromEntry(entry, occInfo);
            if (candidate != null) {
                result.add(candidate);
            }
        }
        return result;
    }

    private CustomerStatementCandidateItemVO buildCandidateFromEntry(LedgerEntryDO entry, Map<String, Object> occInfo) {
        BigDecimal originalAmount = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
        String cat = entry.getCategory();

        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;
        BigDecimal netAmount = originalAmount;
        String direction;
        String signStatus = null;
        String refNo = null;
        String summary = StrUtil.isNotBlank(entry.getRemark()) ? entry.getRemark() : "";

        if (FinanceLedgerCategoryEnum.AR_INC.getValue().equals(cat)) {
            direction = FinanceLedgerDirectionEnum.DEBIT.getValue();
            // 计算红冲净额
            if (entry.getId() != null) {
                List<LedgerEntryDO> cancellations = ledgerEntryMapper.selectListByReversalOfId(entry.getId());
                if (CollUtil.isNotEmpty(cancellations)) {
                    BigDecimal cancelSum = cancellations.stream()
                            .map(c -> c.getOriginalAmount() != null ? c.getOriginalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    netAmount = originalAmount.subtract(cancelSum);
                }
            }
            if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
                // 完全被红冲作废，不呈现
                return null;
            }
            debitAmount = netAmount;

            // 签收状态标记与单号
            if (entry.getRefId() != null) {
                try {
                    ShipmentDO shipment = shipmentMapper.selectById(entry.getRefId());
                    if (shipment != null) {
                        signStatus = shipment.getSignStatus();
                        refNo = shipment.getShipmentNo();
                    }
                } catch (Exception ignore) {
                }
            }
            if (refNo == null && StrUtil.isNotBlank(entry.getRemark())) {
                refNo = entry.getRemark().split(" - ")[0];
            }
        } else {
            direction = FinanceLedgerDirectionEnum.CREDIT.getValue();
            creditAmount = originalAmount;
            if (FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue().equals(cat)) {
                if (StrUtil.isBlank(summary)) summary = "收款核销";
                if (entry.getRefId() != null) {
                    try {
                        PaymentDO payment = paymentMapper.selectById(entry.getRefId());
                        if (payment != null) refNo = payment.getPaymentNo();
                    } catch (Exception ignore) {
                    }
                }
            } else if (FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue().equals(cat)) {
                if (StrUtil.isBlank(summary)) summary = "预收转核销";
                if (entry.getRefId() != null) {
                    try {
                        PaymentDO payment = paymentMapper.selectById(entry.getRefId());
                        if (payment != null) refNo = payment.getPaymentNo();
                    } catch (Exception ignore) {
                    }
                }
            } else if (FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue().equals(cat)) {
                if (StrUtil.isBlank(summary)) summary = "销售退货冲减";
            }
        }

        boolean occupied = false;
        String occupiedStatementNo = null;
        String occupiedStatus = null;
        if (occInfo != null) {
            occupied = true;
            occupiedStatementNo = occInfo.get("statementNo") != null ? occInfo.get("statementNo").toString() : null;
            occupiedStatus = occInfo.get("status") != null ? occInfo.get("status").toString() : null;
        }

        return CustomerStatementCandidateItemVO.builder()
                .ledgerEntryId(entry.getId())
                .bizDate(entry.getBizDate())
                .direction(direction)
                .category(cat)
                .currency(entry.getCurrency())
                .refType(entry.getRefType())
                .refId(entry.getRefId())
                .refNo(refNo)
                .summary(summary)
                .amount(netAmount)
                .debitAmount(debitAmount)
                .creditAmount(creditAmount)
                .signStatus(signStatus)
                .occupied(occupied)
                .occupiedStatementNo(occupiedStatementNo)
                .occupiedStatus(occupiedStatus)
                .build();
    }

    private String generateStatementNoWithRetry() {
        LocalDate today = LocalDate.now();
        String prefix = STATEMENT_NO_PREFIX + today.format(YEAR_FORMATTER);
        List<CustomerStatementDO> candidates = customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix);

        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        if (CollUtil.isNotEmpty(candidates)) {
            for (CustomerStatementDO candidate : candidates) {
                String no = candidate.getStatementNo();
                if (no == null || no.length() <= prefix.length()) {
                    continue;
                }
                String suffix = no.substring(prefix.length());
                Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
                if (!matcher.find()) {
                    continue;
                }
                String digits = matcher.group();
                long num;
                try {
                    num = Long.parseLong(digits);
                } catch (NumberFormatException ignore) {
                    continue;
                }
                if (num > maxNum) {
                    maxNum = num;
                    if (digits.length() > maxDigits) {
                        maxDigits = digits.length();
                    }
                }
            }
        }

        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            maxNum++;
            String candidateNo = prefix + String.format("%0" + maxDigits + "d", maxNum);
            CustomerStatementDO existing = customerStatementMapper.selectByStatementNoIncludingDeleted(candidateNo);
            if (existing == null) {
                return candidateNo;
            }
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    private CustomerStatementRespVO convertToRespVO(CustomerStatementDO statement) {
        CustomerStatementRespVO vo = new CustomerStatementRespVO();
        copyPropertiesToRespVO(statement, vo);
        return vo;
    }

    private CustomerStatementItemRespVO convertItemToRespVO(CustomerStatementItemDO item) {
        return CustomerStatementItemRespVO.builder()
                .id(item.getId())
                .statementId(item.getStatementId())
                .lineNo(item.getLineNo())
                .ledgerEntryId(item.getLedgerEntryId())
                .direction(item.getDirection())
                .bizDate(item.getBizDate())
                .refType(item.getRefType())
                .refNo(item.getRefNo())
                .summary(item.getSummary())
                .debitAmount(item.getDebitAmount())
                .creditAmount(item.getCreditAmount())
                .signStatus(item.getSignStatus())
                .remark(item.getRemark())
                .build();
    }

    private void copyPropertiesToRespVO(CustomerStatementDO statement, CustomerStatementRespVO vo) {
        vo.setId(statement.getId());
        vo.setStatementNo(statement.getStatementNo());
        vo.setCustomerId(statement.getCustomerId());
        vo.setCustomerName(statement.getCustomerName());
        vo.setCurrency(statement.getCurrency());
        vo.setPeriodStart(statement.getPeriodStart());
        vo.setPeriodEnd(statement.getPeriodEnd());
        vo.setOpeningBalance(statement.getOpeningBalance());
        vo.setPeriodDebit(statement.getPeriodDebit());
        vo.setPeriodCredit(statement.getPeriodCredit());
        vo.setClosingBalance(statement.getClosingBalance());
        vo.setStatus(statement.getStatus());
        vo.setConfirmType(statement.getConfirmType());
        vo.setConfirmByName(statement.getConfirmByName());
        vo.setConfirmTime(statement.getConfirmTime());
        vo.setConfirmIp(statement.getConfirmIp());
        vo.setConfirmMethod(statement.getConfirmMethod());
        vo.setRejectReason(statement.getRejectReason());
        vo.setReplyRemark(statement.getReplyRemark());
        vo.setRemark(statement.getRemark());
        vo.setCreateTime(statement.getCreateTime());
        vo.setCreator(statement.getCreator());
    }

}

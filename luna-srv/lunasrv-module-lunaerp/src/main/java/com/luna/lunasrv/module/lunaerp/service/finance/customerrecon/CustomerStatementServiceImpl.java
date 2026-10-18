package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 客户对账单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class CustomerStatementServiceImpl implements CustomerStatementService {

    private static final String STATEMENT_NO_PREFIX = "CS";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final int NO_MIN_DIGITS = 4;
    private static final int MAX_GENERATE_RETRY = 10;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final Set<String> CREDIT_CATEGORIES = Set.of(
            LedgerCategory.RECEIPT_WRITE_OFF.getValue(),
            LedgerCategory.ADVANCE_WRITE_OFF.getValue(),
            LedgerCategory.AR_RETURN_REVERSAL.getValue()
    );

    @Resource
    private CustomerStatementMapper customerStatementMapper;

    @Resource
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private com.luna.lunasrv.module.infra.api.config.ConfigApi configApi;

    @Override
    public CustomerStatementPrefetchRespVO prefetchLines(CustomerStatementPrefetchReqVO reqVO) {
        // 1. 入参守卫
        validatePeriod(reqVO.getPeriodStart(), reqVO.getPeriodEnd());
        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }

        // 2. 计算期初余额（起始日前未核销 AR 余额）
        BigDecimal openingBalance = calculateOpeningBalance(reqVO.getCustomerId(), reqVO.getCurrency(), reqVO.getPeriodStart());

        // 3. 提取期间内候选分录
        List<CandidateEntryHolder> candidates = loadCandidateEntries(reqVO.getCustomerId(), reqVO.getCurrency(),
                reqVO.getPeriodStart(), reqVO.getPeriodEnd(), Collections.emptyList(), null);

        // 4. 统计借贷合计与期末余额
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        List<CustomerStatementCandidateItemRespVO> lines = new ArrayList<>();

        for (CandidateEntryHolder holder : candidates) {
            // CONFIRMED 强占用：完全排除候选行
            if (Boolean.TRUE.equals(holder.isOccupied) && CustomerStatementStatus.CONFIRMED.getValue().equals(holder.occupiedStatus)) {
                continue;
            }
            // 未被占用的分录才计入默认借贷汇总
            if (!Boolean.TRUE.equals(holder.isOccupied)) {
                if ("DEBIT".equalsIgnoreCase(holder.direction)) {
                    periodDebit = periodDebit.add(holder.debitAmount);
                } else {
                    periodCredit = periodCredit.add(holder.creditAmount);
                }
            }
            // DRAFT/SENT 弱占用分录与未占用分录均包含在候选列表中（供前端向导展示占用提示并置灰）
            lines.add(holder.toRespVO());
        }

        periodDebit = periodDebit.setScale(2, RoundingMode.HALF_UP);
        periodCredit = periodCredit.setScale(2, RoundingMode.HALF_UP);
        BigDecimal closingBalance = openingBalance.add(periodDebit).subtract(periodCredit).setScale(2, RoundingMode.HALF_UP);

        // 5. 拼装返回
        CustomerStatementPrefetchRespVO respVO = new CustomerStatementPrefetchRespVO();
        respVO.setCustomerId(customer.getId());
        respVO.setCustomerName(customer.getCompanyName());
        respVO.setCurrency(reqVO.getCurrency());
        respVO.setPeriodStart(reqVO.getPeriodStart());
        respVO.setPeriodEnd(reqVO.getPeriodEnd());
        respVO.setOpeningBalance(openingBalance);
        respVO.setPeriodDebit(periodDebit);
        respVO.setPeriodCredit(periodCredit);
        respVO.setClosingBalance(closingBalance);
        respVO.setLines(lines);

        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "CREATE", bizNo = "{{#_ret}}", success = "生成客户对账单【{{#_ret}}】成功")
    public Long createCustomerStatement(CustomerStatementCreateReqVO createReqVO) {
        // 1. 入参校验
        validatePeriod(createReqVO.getPeriodStart(), createReqVO.getPeriodEnd());
        CustomerDO customer = customerMapper.selectById(createReqVO.getCustomerId());
        if (customer == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }

        // 2. 加载候选分录行（含手工追加分录）
        List<CandidateEntryHolder> candidates = loadCandidateEntries(createReqVO.getCustomerId(), createReqVO.getCurrency(),
                createReqVO.getPeriodStart(), createReqVO.getPeriodEnd(), createReqVO.getManualEntryIds(), null);

        // 过滤可用行：若前端向导传了 selectedEntryIds，则以此为准；若手工指定 manualEntryIds 被 CONFIRMED 占用则抛异常
        List<CandidateEntryHolder> validLines = new ArrayList<>();
        Set<Long> manualEntryIdSet = CollUtil.isNotEmpty(createReqVO.getManualEntryIds())
                ? new HashSet<>(createReqVO.getManualEntryIds()) : Collections.emptySet();
        Set<Long> selectedEntryIdSet = CollUtil.isNotEmpty(createReqVO.getSelectedEntryIds())
                ? new HashSet<>(createReqVO.getSelectedEntryIds()) : null;

        for (CandidateEntryHolder holder : candidates) {
            // 前端向导显式选择过滤
            if (selectedEntryIdSet != null && !selectedEntryIdSet.contains(holder.ledgerEntryId)) {
                continue;
            }
            if (manualEntryIdSet.contains(holder.ledgerEntryId)) {
                if (Boolean.TRUE.equals(holder.isOccupied) && CustomerStatementStatus.CONFIRMED.getValue().equals(holder.occupiedStatus)) {
                    throw exception(STATEMENT_ENTRY_OCCUPIED);
                }
                validLines.add(holder);
            } else {
                if (!Boolean.TRUE.equals(holder.isOccupied)) {
                    validLines.add(holder);
                }
            }
        }

        if (validLines.isEmpty()) {
            throw exception(STATEMENT_NO_PERIOD_ENTRIES);
        }

        // 3. 计算期初、借贷发生额与期末余额
        BigDecimal openingBalance = calculateOpeningBalance(createReqVO.getCustomerId(), createReqVO.getCurrency(), createReqVO.getPeriodStart());
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;

        for (CandidateEntryHolder line : validLines) {
            if ("DEBIT".equalsIgnoreCase(line.direction)) {
                periodDebit = periodDebit.add(line.debitAmount);
            } else {
                periodCredit = periodCredit.add(line.creditAmount);
            }
        }

        periodDebit = periodDebit.setScale(2, RoundingMode.HALF_UP);
        periodCredit = periodCredit.setScale(2, RoundingMode.HALF_UP);
        BigDecimal closingBalance = openingBalance.add(periodDebit).subtract(periodCredit).setScale(2, RoundingMode.HALF_UP);

        // 4. 单号发号防撞
        String statementNo = StrUtil.isNotBlank(createReqVO.getStatementNo())
                ? createReqVO.getStatementNo().trim()
                : generateStatementNo(createReqVO.getPeriodEnd() != null ? createReqVO.getPeriodEnd() : LocalDate.now());

        // 5. 插入对账单主表
        CustomerStatementDO statementDO = new CustomerStatementDO();
        statementDO.setStatementNo(statementNo);
        statementDO.setCustomerId(customer.getId());
        statementDO.setCustomerName(customer.getCompanyName());
        statementDO.setCurrency(createReqVO.getCurrency());
        statementDO.setPeriodStart(createReqVO.getPeriodStart());
        statementDO.setPeriodEnd(createReqVO.getPeriodEnd());
        statementDO.setOpeningBalance(openingBalance);
        statementDO.setPeriodDebit(periodDebit);
        statementDO.setPeriodCredit(periodCredit);
        statementDO.setClosingBalance(closingBalance);
        statementDO.setStatus(CustomerStatementStatus.DRAFT.getValue());
        statementDO.setRemark(createReqVO.getRemark());

        customerStatementMapper.insert(statementDO);

        // 6. 批量插入明细行快照
        int lineNo = 1;
        for (CandidateEntryHolder line : validLines) {
            CustomerStatementItemDO itemDO = new CustomerStatementItemDO();
            itemDO.setStatementId(statementDO.getId());
            itemDO.setLineNo(lineNo++);
            itemDO.setLedgerEntryId(line.ledgerEntryId);
            itemDO.setDirection(line.direction);
            itemDO.setBizDate(line.bizDate);
            itemDO.setRefType(line.refType);
            itemDO.setRefNo(line.refNo);
            itemDO.setSummary(line.summary);
            itemDO.setDebitAmount(line.debitAmount);
            itemDO.setCreditAmount(line.creditAmount);
            itemDO.setSignStatus(line.signStatus);
            itemDO.setRemark(line.remark);

            customerStatementItemMapper.insert(itemDO);
        }

        return statementDO.getId();
    }

    @Override
    public CustomerStatementDetailRespVO getCustomerStatement(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        validateStatementExistsAndTenant(statement);

        List<CustomerStatementItemDO> items = customerStatementItemMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .eq(CustomerStatementItemDO::getStatementId, id)
                        .orderByAsc(CustomerStatementItemDO::getLineNo)
                        .orderByAsc(CustomerStatementItemDO::getId));

        CustomerStatementDetailRespVO detailVO = BeanUtils.toBean(statement, CustomerStatementDetailRespVO.class);
        List<CustomerStatementItemRespVO> itemVOs = BeanUtils.toBean(items, CustomerStatementItemRespVO.class);

        // 如果单据处于 SENT 状态，检查各借方明细分录是否有后续红冲分录（AR_INC_REVERSAL）
        if (CustomerStatementStatus.SENT.getValue().equals(statement.getStatus()) && CollUtil.isNotEmpty(items)) {
            List<Long> ledgerEntryIds = items.stream()
                    .filter(i -> "DEBIT".equalsIgnoreCase(i.getDirection()) && i.getLedgerEntryId() != null)
                    .map(CustomerStatementItemDO::getLedgerEntryId)
                    .toList();
            if (CollUtil.isNotEmpty(ledgerEntryIds)) {
                List<LedgerEntryDO> reversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                        .inOrEmpty(LedgerEntryDO::getReversalOfId, ledgerEntryIds));
                Set<Long> reversedEntryIdSet = reversals.stream()
                        .map(LedgerEntryDO::getReversalOfId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                for (CustomerStatementItemRespVO itemVO : itemVOs) {
                    if (itemVO.getLedgerEntryId() != null && reversedEntryIdSet.contains(itemVO.getLedgerEntryId())) {
                        itemVO.setHasReversal(true);
                    } else {
                        itemVO.setHasReversal(false);
                    }
                }
            }
        }

        detailVO.setItems(itemVOs);
        return detailVO;
    }

    @Override
    public PageResult<CustomerStatementRespVO> getCustomerStatementPage(CustomerStatementPageReqVO pageReqVO) {
        LambdaQueryWrapperX<CustomerStatementDO> wrapper = new LambdaQueryWrapperX<CustomerStatementDO>()
                .likeIfPresent(CustomerStatementDO::getStatementNo, pageReqVO.getStatementNo())
                .eqIfPresent(CustomerStatementDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(CustomerStatementDO::getCustomerName, pageReqVO.getCustomerName())
                .eqIfPresent(CustomerStatementDO::getCurrency, pageReqVO.getCurrency())
                .eqIfPresent(CustomerStatementDO::getStatus, pageReqVO.getStatus())
                .geIfPresent(CustomerStatementDO::getPeriodStart, pageReqVO.getPeriodStart())
                .leIfPresent(CustomerStatementDO::getPeriodEnd, pageReqVO.getPeriodEnd())
                .orderByDesc(CustomerStatementDO::getId);

        PageResult<CustomerStatementDO> pageResult = customerStatementMapper.selectPage(pageReqVO, wrapper);
        return BeanUtils.toBean(pageResult, CustomerStatementRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}", success = "修改客户对账单【{{#updateReqVO.id}}】成功")
    public void updateCustomerStatement(CustomerStatementUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(updateReqVO.getId());
        validateStatementExistsAndTenant(statement);

        // 仅 DRAFT 态允许修改
        if (!CustomerStatementStatus.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        // 1. 查询当前对账单所有行
        List<CustomerStatementItemDO> allItems = customerStatementItemMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .eq(CustomerStatementItemDO::getStatementId, statement.getId())
                        .orderByAsc(CustomerStatementItemDO::getLineNo));

        Set<Long> toDeleteIdSet = CollUtil.isNotEmpty(updateReqVO.getDeletedItemIds())
                ? new HashSet<>(updateReqVO.getDeletedItemIds()) : Collections.emptySet();

        List<Long> validDeleteIds = new ArrayList<>();
        List<CustomerStatementItemDO> remainingItems = new ArrayList<>();

        for (CustomerStatementItemDO item : allItems) {
            if (toDeleteIdSet.contains(item.getId())) {
                validDeleteIds.add(item.getId());
            } else {
                remainingItems.add(item);
            }
        }

        if (!validDeleteIds.isEmpty()) {
            customerStatementItemMapper.deleteBatchIds(validDeleteIds);
        }

        // 2. 重新汇总并更新借贷发生额与期末余额
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        for (CustomerStatementItemDO item : remainingItems) {
            if ("DEBIT".equalsIgnoreCase(item.getDirection())) {
                periodDebit = periodDebit.add(item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO);
            } else {
                periodCredit = periodCredit.add(item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO);
            }
        }

        periodDebit = periodDebit.setScale(2, RoundingMode.HALF_UP);
        periodCredit = periodCredit.setScale(2, RoundingMode.HALF_UP);
        BigDecimal opening = statement.getOpeningBalance() != null ? statement.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal closingBalance = opening.add(periodDebit).subtract(periodCredit).setScale(2, RoundingMode.HALF_UP);

        statement.setPeriodDebit(periodDebit);
        statement.setPeriodCredit(periodCredit);
        statement.setClosingBalance(closingBalance);
        statement.setRemark(updateReqVO.getRemark());

        customerStatementMapper.updateById(statement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "DELETE", bizNo = "{{#id}}", success = "删除客户对账单【{{#id}}】成功")
    public void deleteCustomerStatement(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        validateStatementExistsAndTenant(statement);

        // 仅 DRAFT 态允许软删除
        if (!CustomerStatementStatus.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        customerStatementMapper.deleteById(id);
        customerStatementItemMapper.delete(new LambdaQueryWrapperX<CustomerStatementItemDO>()
                .eq(CustomerStatementItemDO::getStatementId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "ADD_ENTRY", bizNo = "{{#addEntryReqVO.statementId}}", success = "对账单【{{#addEntryReqVO.statementId}}】手工追加分录【{{#addEntryReqVO.ledgerEntryId}}】成功")
    public Long addStatementEntry(CustomerStatementAddEntryReqVO addEntryReqVO) {
        if (addEntryReqVO == null || addEntryReqVO.getStatementId() == null || addEntryReqVO.getLedgerEntryId() == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(addEntryReqVO.getStatementId());
        validateStatementExistsAndTenant(statement);

        // 仅 DRAFT 态允许追加
        if (!CustomerStatementStatus.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        LedgerEntryDO entry = ledgerEntryMapper.selectById(addEntryReqVO.getLedgerEntryId());
        if (entry == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }

        // 守卫：客户一致性
        if (!Objects.equals(statement.getCustomerId(), entry.getPartyId()) || !"CUSTOMER".equalsIgnoreCase(entry.getPartyType())) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }

        // 守卫：币种一致性
        if (!Objects.equals(statement.getCurrency(), entry.getCurrency())) {
            throw exception(STATEMENT_CURRENCY_MIXED);
        }

        // 守卫：已在当前对账单中
        Long existingInCurrentCount = customerStatementItemMapper.selectCount(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .eq(CustomerStatementItemDO::getStatementId, statement.getId())
                        .eq(CustomerStatementItemDO::getLedgerEntryId, entry.getId()));
        if (existingInCurrentCount != null && existingInCurrentCount > 0) {
            throw exception(STATEMENT_ENTRY_OCCUPIED);
        }

        // 守卫：已被 CONFIRMED 对账单占用
        Map<Long, CustomerStatementDO> occupiedMap = batchLoadOccupiedStatementMap(List.of(entry.getId()));
        CustomerStatementDO occupiedStatement = occupiedMap.get(entry.getId());
        if (occupiedStatement != null && CustomerStatementStatus.CONFIRMED.getValue().equals(occupiedStatement.getStatus())) {
            throw exception(STATEMENT_ENTRY_OCCUPIED);
        }

        // 计算当前明细行号
        List<CustomerStatementItemDO> existingItems = customerStatementItemMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .eq(CustomerStatementItemDO::getStatementId, statement.getId())
                        .orderByDesc(CustomerStatementItemDO::getLineNo));
        int maxLineNo = CollUtil.isNotEmpty(existingItems) && existingItems.get(0).getLineNo() != null
                ? existingItems.get(0).getLineNo() : 0;

        // 计算金额快照（如是发货分录，扣减其红冲额）
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;
        String signStatus = null;
        String refNo = "";

        if ("DEBIT".equalsIgnoreCase(entry.getDirection())) {
            BigDecimal orig = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            List<LedgerEntryDO> reversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                    .eq(LedgerEntryDO::getReversalOfId, entry.getId()));
            BigDecimal revTotal = reversals.stream().map(r -> r.getOriginalAmount() != null ? r.getOriginalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            debitAmount = orig.subtract(revTotal);
            if (debitAmount.compareTo(BigDecimal.ZERO) < 0) {
                debitAmount = BigDecimal.ZERO;
            }
        } else {
            creditAmount = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
        }

        // 加载关联单号与发货签收标记
        if (entry.getRefId() != null) {
            if ("SHIPMENT_ITEM".equalsIgnoreCase(entry.getRefType()) || "SHIPMENT_FREIGHT".equalsIgnoreCase(entry.getRefType())) {
                ShipmentDO shipment = shipmentMapper.selectById(entry.getRefId());
                if (shipment != null) {
                    refNo = shipment.getShipmentNo();
                    signStatus = shipment.getSignStatus();
                }
            } else if ("RECEIPT".equalsIgnoreCase(entry.getRefType())) {
                PaymentDO payment = paymentMapper.selectById(entry.getRefId());
                if (payment != null) {
                    refNo = payment.getPaymentNo();
                }
            }
        }

        CustomerStatementItemDO newItem = new CustomerStatementItemDO();
        newItem.setStatementId(statement.getId());
        newItem.setLineNo(maxLineNo + 1);
        newItem.setLedgerEntryId(entry.getId());
        newItem.setDirection(entry.getDirection());
        newItem.setBizDate(entry.getBizDate());
        newItem.setRefType(entry.getRefType());
        newItem.setRefNo(refNo);
        newItem.setSummary(StrUtil.isNotBlank(entry.getRemark()) ? entry.getRemark() : entry.getCategory());
        newItem.setDebitAmount(debitAmount.setScale(2, RoundingMode.HALF_UP));
        newItem.setCreditAmount(creditAmount.setScale(2, RoundingMode.HALF_UP));
        newItem.setSignStatus(signStatus);
        newItem.setRemark(addEntryReqVO.getRemark());

        customerStatementItemMapper.insert(newItem);

        // 重新计算并更新主表汇总
        // 若追加的是起始日前的历史分录，从期初余额中扣除对应净额，防止与本期明细累加导致期末双计
        BigDecimal newOpening = statement.getOpeningBalance() != null ? statement.getOpeningBalance() : BigDecimal.ZERO;
        if (entry.getBizDate() != null && statement.getPeriodStart() != null && entry.getBizDate().isBefore(statement.getPeriodStart())) {
            if ("DEBIT".equalsIgnoreCase(newItem.getDirection())) {
                newOpening = newOpening.subtract(newItem.getDebitAmount()).setScale(2, RoundingMode.HALF_UP);
            } else {
                newOpening = newOpening.add(newItem.getCreditAmount()).setScale(2, RoundingMode.HALF_UP);
            }
            statement.setOpeningBalance(newOpening);
        }

        BigDecimal newDebit = statement.getPeriodDebit().add(newItem.getDebitAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newCredit = statement.getPeriodCredit().add(newItem.getCreditAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newClosing = newOpening.add(newDebit).subtract(newCredit).setScale(2, RoundingMode.HALF_UP);

        statement.setPeriodDebit(newDebit);
        statement.setPeriodCredit(newCredit);
        statement.setClosingBalance(newClosing);
        customerStatementMapper.updateById(statement);

        return newItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "SEND", bizNo = "{{#id}}", success = "发送客户对账单【{{#id}}】成功")
    public void sendCustomerStatement(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        validateStatementExistsAndTenant(statement);

        if (!CustomerStatementStatus.DRAFT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setStatus(CustomerStatementStatus.SENT.getValue());

        int affected = customerStatementMapper.update(updateDO,
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .eq(CustomerStatementDO::getId, id)
                        .eq(CustomerStatementDO::getStatus, CustomerStatementStatus.DRAFT.getValue()));
        if (affected == 0) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "VOID", bizNo = "{{#id}}", success = "作废客户对账单【{{#id}}】成功")
    public void voidCustomerStatement(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        validateStatementExistsAndTenant(statement);

        if (CustomerStatementStatus.CONFIRMED.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_CONFIRMED_NOT_VOID);
        }
        if (CustomerStatementStatus.VOID.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
        if (!CustomerStatementStatus.DRAFT.getValue().equals(statement.getStatus())
                && !CustomerStatementStatus.SENT.getValue().equals(statement.getStatus())
                && !CustomerStatementStatus.REJECTED.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setStatus(CustomerStatementStatus.VOID.getValue());

        int affected = customerStatementMapper.update(updateDO,
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .eq(CustomerStatementDO::getId, id)
                        .in(CustomerStatementDO::getStatus,
                                CustomerStatementStatus.DRAFT.getValue(),
                                CustomerStatementStatus.SENT.getValue(),
                                CustomerStatementStatus.REJECTED.getValue()));
        if (affected == 0) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "CONFIRM_ON_BEHALF", bizNo = "{{#reqVO.id}}", success = "代客确认客户对账单【{{#reqVO.id}}】成功")
    public void confirmOnBehalf(CustomerStatementConfirmOnBehalfReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        if (cn.hutool.core.util.StrUtil.isBlank(reqVO.getConfirmMethod()) || cn.hutool.core.util.StrUtil.isBlank(reqVO.getRemark())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(reqVO.getId());
        validateStatementExistsAndTenant(statement);

        if (!CustomerStatementStatus.SENT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        String confirmByName = "线下代办";
        try {
            String nickname = com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname();
            if (cn.hutool.core.util.StrUtil.isNotBlank(nickname)) {
                confirmByName = nickname;
            } else {
                Long loginUserId = com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
                if (loginUserId != null) {
                    confirmByName = "用户#" + loginUserId;
                }
            }
        } catch (Exception ignored) {
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setStatus(CustomerStatementStatus.CONFIRMED.getValue());
        updateDO.setConfirmType(com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementConfirmType.ON_BEHALF.getValue());
        updateDO.setConfirmByName(confirmByName);
        updateDO.setConfirmTime(java.time.LocalDateTime.now());
        updateDO.setConfirmMethod(reqVO.getConfirmMethod());
        if (reqVO.getRemark() != null) {
            updateDO.setRemark(reqVO.getRemark());
        }

        int affected = customerStatementMapper.update(updateDO,
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .eq(CustomerStatementDO::getId, reqVO.getId())
                        .eq(CustomerStatementDO::getStatus, CustomerStatementStatus.SENT.getValue()));
        if (affected == 0) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "REJECT", bizNo = "{{#reqVO.id}}", success = "驳回客户对账单【{{#reqVO.id}}】成功")
    public void rejectCustomerStatement(CustomerStatementRejectReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(reqVO.getId());
        validateStatementExistsAndTenant(statement);

        if (!CustomerStatementStatus.SENT.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setStatus(CustomerStatementStatus.REJECTED.getValue());
        updateDO.setRejectReason(reqVO.getRejectReason());

        int affected = customerStatementMapper.update(updateDO,
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .eq(CustomerStatementDO::getId, reqVO.getId())
                        .eq(CustomerStatementDO::getStatus, CustomerStatementStatus.SENT.getValue()));
        if (affected == 0) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_STATEMENT", subType = "REJECT_REPLY", bizNo = "{{#reqVO.id}}", success = "回复客户对账单【{{#reqVO.id}}】异议并重置为草稿成功")
    public void rejectReplyCustomerStatement(CustomerStatementRejectReplyReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(reqVO.getId());
        validateStatementExistsAndTenant(statement);

        if (!CustomerStatementStatus.REJECTED.getValue().equals(statement.getStatus())) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setStatus(CustomerStatementStatus.DRAFT.getValue());
        updateDO.setReplyRemark(reqVO.getReplyRemark());

        int affected = customerStatementMapper.update(updateDO,
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .eq(CustomerStatementDO::getId, reqVO.getId())
                        .eq(CustomerStatementDO::getStatus, CustomerStatementStatus.REJECTED.getValue()));
        if (affected == 0) {
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @LogRecord(type = "FINANCE_STATEMENT", subType = "PRINT", bizNo = "{{#id}}", success = "打印客户对账单【{{#id}}】成功")
    public CustomerStatementPrintRespVO getCustomerStatementPrintData(Long id) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        CustomerStatementDO statement = customerStatementMapper.selectById(id);
        validateStatementExistsAndTenant(statement);

        CustomerDO customer = customerMapper.selectById(statement.getCustomerId());

        String customerName = statement.getCustomerName();
        String taxNo = null;
        String address = null;
        String bankName = null;
        String bankAccountNo = null;
        String contactPhone = null;

        if (customer != null) {
            if (StrUtil.isNotBlank(customer.getInvoiceTitle())) {
                customerName = customer.getInvoiceTitle();
            } else if (StrUtil.isNotBlank(customer.getCompanyName())) {
                customerName = customer.getCompanyName();
            }
            taxNo = customer.getUnifiedSocialCreditCode();
            address = StrUtil.isNotBlank(customer.getInvoiceAddress()) ? customer.getInvoiceAddress() : customer.getAddress();
            bankName = customer.getBankName();
            bankAccountNo = customer.getBankAccountNo();
            contactPhone = customer.getPhone();
        }

        String qrUrl = null;
        String baseUrl = configApi != null ? configApi.getConfigValueByKey("luna.portal.base-url") : null;
        if (StrUtil.isNotBlank(baseUrl)) {
            String cleanBaseUrl = baseUrl.trim().replaceAll("/+$", "");
            qrUrl = cleanBaseUrl + "/portal/statement?no=" + statement.getStatementNo();
        }

        List<CustomerStatementItemDO> items = customerStatementItemMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .eq(CustomerStatementItemDO::getStatementId, id)
                        .orderByAsc(CustomerStatementItemDO::getLineNo)
                        .orderByAsc(CustomerStatementItemDO::getId));

        List<CustomerStatementPrintRespVO.ItemVO> printItems = new ArrayList<>();
        if (CollUtil.isNotEmpty(items)) {
            for (CustomerStatementItemDO item : items) {
                printItems.add(CustomerStatementPrintRespVO.ItemVO.builder()
                        .lineNo(item.getLineNo())
                        .bizDate(item.getBizDate())
                        .direction(item.getDirection())
                        .refType(item.getRefType())
                        .refNo(item.getRefNo())
                        .summary(item.getSummary())
                        .debitAmount(item.getDebitAmount())
                        .creditAmount(item.getCreditAmount())
                        .signStatus(item.getSignStatus())
                        .remark(item.getRemark())
                        .build());
            }
        }

        return CustomerStatementPrintRespVO.builder()
                .bizType("FINANCE_STATEMENT")
                .title("客户对账单")
                .id(statement.getId())
                .statementNo(statement.getStatementNo())
                .customerId(statement.getCustomerId())
                .customerName(customerName)
                .taxNo(taxNo)
                .address(address)
                .bankName(bankName)
                .bankAccountNo(bankAccountNo)
                .contactPhone(contactPhone)
                .currency(statement.getCurrency())
                .periodStart(statement.getPeriodStart())
                .periodEnd(statement.getPeriodEnd())
                .openingBalance(statement.getOpeningBalance())
                .periodDebit(statement.getPeriodDebit())
                .periodCredit(statement.getPeriodCredit())
                .closingBalance(statement.getClosingBalance())
                .status(statement.getStatus())
                .confirmType(statement.getConfirmType())
                .confirmByName(statement.getConfirmByName())
                .confirmTime(statement.getConfirmTime())
                .confirmMethod(statement.getConfirmMethod())
                .qrUrl(qrUrl)
                .remark(statement.getRemark())
                .creator(statement.getCreator())
                .createTime(statement.getCreateTime())
                .printTime(java.time.LocalDateTime.now())
                .items(printItems)
                .build();
    }

    // ==================== 私有辅助计算与防 N+1 批量快照方法 ====================

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)) {
            throw exception(STATEMENT_PERIOD_INVALID);
        }
    }

    private void validateStatementExistsAndTenant(CustomerStatementDO statement) {
        if (statement == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && statement.getTenantId() != null && !currentTenantId.equals(statement.getTenantId())) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
    }

    private BigDecimal calculateOpeningBalance(Long customerId, String currency, LocalDate periodStart) {
        // 查询起始日前该客户该币种的全量分录
        List<LedgerEntryDO> priorEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .eq(LedgerEntryDO::getPartyId, customerId)
                .eq(LedgerEntryDO::getCurrency, currency)
                .lt(LedgerEntryDO::getBizDate, periodStart));

        if (CollUtil.isEmpty(priorEntries)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal priorArInc = BigDecimal.ZERO;
        BigDecimal priorArReversal = BigDecimal.ZERO;
        BigDecimal priorCredit = BigDecimal.ZERO;

        for (LedgerEntryDO entry : priorEntries) {
            BigDecimal amt = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            String cat = entry.getCategory();

            if (LedgerCategory.AR_INC.getValue().equalsIgnoreCase(cat)) {
                priorArInc = priorArInc.add(amt);
            } else if (LedgerCategory.AR_INC_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                priorArReversal = priorArReversal.add(amt);
            } else if (CREDIT_CATEGORIES.contains(cat)) {
                priorCredit = priorCredit.add(amt);
            }
        }

        // 期初未核销 AR 余额 = (借方 AR_INC - 红冲 AR_INC_REVERSAL) - 贷方核销/退货冲减
        return priorArInc.subtract(priorArReversal).subtract(priorCredit).setScale(2, RoundingMode.HALF_UP);
    }

    private List<CandidateEntryHolder> loadCandidateEntries(Long customerId, String currency,
                                                            LocalDate periodStart, LocalDate periodEnd,
                                                            Collection<Long> manualEntryIds, Long excludeStatementId) {
        // 1. 查询区间内的借方与贷方分录
        List<LedgerEntryDO> periodEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .eq(LedgerEntryDO::getPartyId, customerId)
                .ge(LedgerEntryDO::getBizDate, periodStart)
                .le(LedgerEntryDO::getBizDate, periodEnd)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        // 2. 查询手工指定的分录
        List<LedgerEntryDO> manualEntries = Collections.emptyList();
        if (CollUtil.isNotEmpty(manualEntryIds)) {
            manualEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                    .inOrEmpty(LedgerEntryDO::getId, manualEntryIds));
        }

        // 合并分录并按 ID 去重
        Map<Long, LedgerEntryDO> entryMap = new LinkedHashMap<>();
        for (LedgerEntryDO e : periodEntries) {
            entryMap.put(e.getId(), e);
        }
        for (LedgerEntryDO e : manualEntries) {
            entryMap.put(e.getId(), e);
        }

        if (entryMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 守卫：多币种混入检测
        for (LedgerEntryDO e : entryMap.values()) {
            if (!Objects.equals(currency, e.getCurrency())) {
                throw exception(STATEMENT_CURRENCY_MIXED);
            }
            if (!Objects.equals(customerId, e.getPartyId())) {
                throw exception(STATEMENT_CUSTOMER_MISMATCH);
            }
        }

        // 3. 统计发货红冲分录 (AR_INC_REVERSAL)
        List<Long> arIncIds = entryMap.values().stream()
                .filter(e -> LedgerCategory.AR_INC.getValue().equalsIgnoreCase(e.getCategory()))
                .map(LedgerEntryDO::getId)
                .toList();

        Map<Long, BigDecimal> reversalSumMap = new HashMap<>();
        if (!arIncIds.isEmpty()) {
            List<LedgerEntryDO> reversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                    .inOrEmpty(LedgerEntryDO::getReversalOfId, arIncIds));
            for (LedgerEntryDO r : reversals) {
                if (r.getReversalOfId() != null && r.getOriginalAmount() != null) {
                    reversalSumMap.merge(r.getReversalOfId(), r.getOriginalAmount(), BigDecimal::add);
                }
            }
        }

        // 4. 批量查询被占用情况
        List<Long> allEntryIds = new ArrayList<>(entryMap.keySet());
        Map<Long, CustomerStatementDO> occupiedMap = batchLoadOccupiedStatementMap(allEntryIds);

        // 5. 批量加载发货单与收款单快照
        Set<Long> shipmentIds = new HashSet<>();
        Set<Long> paymentIds = new HashSet<>();
        for (LedgerEntryDO e : entryMap.values()) {
            if (e.getRefId() != null) {
                if ("SHIPMENT_ITEM".equalsIgnoreCase(e.getRefType()) || "SHIPMENT_FREIGHT".equalsIgnoreCase(e.getRefType())) {
                    shipmentIds.add(e.getRefId());
                } else if ("RECEIPT".equalsIgnoreCase(e.getRefType())) {
                    paymentIds.add(e.getRefId());
                }
            }
        }
        Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);
        Map<Long, PaymentDO> paymentMap = batchLoadPaymentMap(paymentIds);

        // 6. 构造 CandidateEntryHolder 列表
        List<CandidateEntryHolder> result = new ArrayList<>();
        for (LedgerEntryDO entry : entryMap.values()) {
            String cat = entry.getCategory();
            // 忽略收款红冲与供应商冲销
            if (LedgerCategory.RECEIPT_REVERSAL.getValue().equalsIgnoreCase(cat)
                    || LedgerCategory.OUT_SUPPLIER_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                continue;
            }

            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;

            if (LedgerCategory.AR_INC.getValue().equalsIgnoreCase(cat)) {
                BigDecimal orig = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
                BigDecimal rev = reversalSumMap.getOrDefault(entry.getId(), BigDecimal.ZERO);
                debitAmount = orig.subtract(rev);
                // 若净额为 0 或负数，表明已完全红冲，排除候选
                if (debitAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
            } else if (LedgerCategory.AR_INC_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                // 如果本期的发货红冲，其对应的原发货分录不在本期 candidates 中（即原发货发生于前期），作为当期贷方冲减入单
                if (entry.getReversalOfId() != null && arIncIds.contains(entry.getReversalOfId())) {
                    continue; // 原发货在当期，已在原分录净额中扣减体现
                }
                creditAmount = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            } else if (CREDIT_CATEGORIES.contains(cat)) {
                creditAmount = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            } else {
                continue;
            }

            CandidateEntryHolder holder = new CandidateEntryHolder();
            holder.ledgerEntryId = entry.getId();
            holder.direction = entry.getDirection();
            holder.bizDate = entry.getBizDate();
            holder.refType = entry.getRefType();
            holder.refId = entry.getRefId();
            holder.refItemId = entry.getRefItemId();
            holder.originalAmount = entry.getOriginalAmount();
            holder.debitAmount = debitAmount.setScale(2, RoundingMode.HALF_UP);
            holder.creditAmount = creditAmount.setScale(2, RoundingMode.HALF_UP);
            holder.remark = entry.getRemark();

            // 业务回指单号与签收标记
            if ("SHIPMENT_ITEM".equalsIgnoreCase(entry.getRefType()) || "SHIPMENT_FREIGHT".equalsIgnoreCase(entry.getRefType())) {
                ShipmentDO shipment = shipmentMap.get(entry.getRefId());
                if (shipment != null) {
                    holder.refNo = shipment.getShipmentNo();
                    holder.signStatus = shipment.getSignStatus();
                }
            } else if ("RECEIPT".equalsIgnoreCase(entry.getRefType())) {
                PaymentDO payment = paymentMap.get(entry.getRefId());
                if (payment != null) {
                    holder.refNo = payment.getPaymentNo();
                }
            }

            // 摘要
            if ("SHIPMENT_FREIGHT".equalsIgnoreCase(entry.getRefType())) {
                holder.summary = "运费";
            } else if (StrUtil.isNotBlank(entry.getRemark())) {
                holder.summary = entry.getRemark();
            } else {
                holder.summary = entry.getCategory();
            }

            // 占用情况判定
            CustomerStatementDO occupiedStmt = occupiedMap.get(entry.getId());
            if (occupiedStmt != null && !Objects.equals(occupiedStmt.getId(), excludeStatementId)) {
                holder.isOccupied = true;
                holder.occupiedStatementNo = occupiedStmt.getStatementNo();
                holder.occupiedStatus = occupiedStmt.getStatus();
            } else {
                holder.isOccupied = false;
            }

            result.add(holder);
        }

        return result;
    }

    private Map<Long, CustomerStatementDO> batchLoadOccupiedStatementMap(Collection<Long> ledgerEntryIds) {
        if (CollUtil.isEmpty(ledgerEntryIds)) {
            return Collections.emptyMap();
        }
        List<CustomerStatementItemDO> items = customerStatementItemMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementItemDO>()
                        .inOrEmpty(CustomerStatementItemDO::getLedgerEntryId, ledgerEntryIds));
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }

        Set<Long> stmtIds = items.stream().map(CustomerStatementItemDO::getStatementId).collect(Collectors.toSet());
        List<CustomerStatementDO> statements = customerStatementMapper.selectList(
                new LambdaQueryWrapperX<CustomerStatementDO>()
                        .inOrEmpty(CustomerStatementDO::getId, stmtIds));
        if (CollUtil.isEmpty(statements)) {
            return Collections.emptyMap();
        }

        // 仅保留未作废（非 VOID）的对账单
        Map<Long, CustomerStatementDO> statementMap = statements.stream()
                .filter(s -> !CustomerStatementStatus.VOID.getValue().equals(s.getStatus()))
                .collect(Collectors.toMap(CustomerStatementDO::getId, s -> s, (oldVal, newVal) -> newVal));

        Map<Long, CustomerStatementDO> result = new HashMap<>();
        for (CustomerStatementItemDO item : items) {
            CustomerStatementDO stmt = statementMap.get(item.getStatementId());
            if (stmt != null) {
                // 如果已有占用，优先保留 CONFIRMED 强占用
                CustomerStatementDO prev = result.get(item.getLedgerEntryId());
                if (prev == null || CustomerStatementStatus.CONFIRMED.getValue().equals(stmt.getStatus())) {
                    result.put(item.getLedgerEntryId(), stmt);
                }
            }
        }
        return result;
    }

    private Map<Long, ShipmentDO> batchLoadShipmentMap(Collection<Long> shipmentIds) {
        if (CollUtil.isEmpty(shipmentIds)) {
            return Collections.emptyMap();
        }
        List<ShipmentDO> shipments = shipmentMapper.selectList(
                new LambdaQueryWrapperX<ShipmentDO>().inOrEmpty(ShipmentDO::getId, shipmentIds));
        return CollectionUtils.convertMap(shipments, ShipmentDO::getId);
    }

    private Map<Long, PaymentDO> batchLoadPaymentMap(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptyMap();
        }
        List<PaymentDO> payments = paymentMapper.selectList(
                new LambdaQueryWrapperX<PaymentDO>().inOrEmpty(PaymentDO::getId, paymentIds));
        return CollectionUtils.convertMap(payments, PaymentDO::getId);
    }

    private String generateStatementNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = STATEMENT_NO_PREFIX + YEAR_FORMATTER.format(targetDate);
        List<CustomerStatementDO> candidates = customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix);

        long maxNum = 0;
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
                try {
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            long nextNum = maxNum + 1 + i;
            String candidateNo = String.format("%s%0" + maxDigits + "d", prefix, nextNum);
            CustomerStatementDO existing = customerStatementMapper.selectByStatementNoIncludingDeleted(candidateNo);
            if (existing == null) {
                return candidateNo;
            }
        }
        log.error("[generateStatementNo] 对账单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(STATEMENT_NO_EXHAUSTED);
    }

    /**
     * 内部候选分录暂存对象
     */
    private static class CandidateEntryHolder {
        Long ledgerEntryId;
        String direction;
        LocalDate bizDate;
        String refType;
        Long refId;
        Long refItemId;
        String refNo;
        String summary;
        BigDecimal originalAmount;
        BigDecimal debitAmount;
        BigDecimal creditAmount;
        String signStatus;
        String remark;
        Boolean isOccupied;
        String occupiedStatementNo;
        String occupiedStatus;

        CustomerStatementCandidateItemRespVO toRespVO() {
            CustomerStatementCandidateItemRespVO vo = new CustomerStatementCandidateItemRespVO();
            vo.setLedgerEntryId(ledgerEntryId);
            vo.setDirection(direction);
            vo.setBizDate(bizDate);
            vo.setRefType(refType);
            vo.setRefId(refId);
            vo.setRefItemId(refItemId);
            vo.setRefNo(refNo);
            vo.setSummary(summary);
            vo.setOriginalAmount(originalAmount);
            vo.setDebitAmount(debitAmount);
            vo.setCreditAmount(creditAmount);
            vo.setSignStatus(signStatus);
            vo.setRemark(remark);
            vo.setIsOccupied(isOccupied);
            vo.setOccupiedStatementNo(occupiedStatementNo);
            vo.setOccupiedStatus(occupiedStatus);
            vo.setEntryType("DEBIT".equalsIgnoreCase(direction) ? "发货应收" : "收款核销/冲减");
            return vo;
        }
    }

}

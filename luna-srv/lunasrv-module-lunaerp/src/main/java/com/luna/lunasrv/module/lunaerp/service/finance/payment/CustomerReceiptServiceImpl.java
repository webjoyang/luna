package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 客户收款单服务实现类（IN 侧）
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class CustomerReceiptServiceImpl implements CustomerReceiptService {

    private static final String RECEIPT_NO_PREFIX = "RC-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("(\\d+)$");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 5;

    public static final String DIRECTION_IN = "IN";
    public static final String REF_TYPE_AR_ENTRY = "AR_ENTRY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REVERSED = "REVERSED";
    public static final String RECEIPT_TYPE_RECEIPT = "RECEIPT";
    public static final String RECEIPT_TYPE_ADVANCE = "ADVANCE";
    public static final String APPROVAL_STATUS_PENDING = "PENDING";
    public static final String APPROVAL_STATUS_APPROVING = "APPROVING";
    public static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    public static final String APPROVAL_STATUS_REJECTED = "REJECTED";

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private AccountService accountService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceipt(ReceiptSaveReqVO reqVO) {
        // 1. 校验客户
        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null) {
            throw exception(RECEIPT_CUSTOMER_MISMATCH);
        }

        // 2. 校验资金账户（必须存在且 ENABLE）
        AccountDO account = accountService.getAccount(reqVO.getAccountId());
        if (account == null || !"ENABLE".equalsIgnoreCase(account.getStatus())) {
            throw exception(ACCOUNT_DISABLED);
        }

        // 3. 校验金额与币种
        if (reqVO.getPayAmount() == null || reqVO.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIPT_ALLOCATE_EXCEED);
        }

        // 4. 校验核销明细（若为 RECEIPT 货款）
        validateReceiptItems(reqVO.getCustomerId(), reqVO.getCurrency(), reqVO.getPayAmount(),
                reqVO.getPrepayUsedAmount(), reqVO.getItems(), null);

        // 5. 生成单号
        String paymentNo;
        if (StrUtil.isNotBlank(reqVO.getPaymentNo())) {
            paymentNo = reqVO.getPaymentNo().trim();
            PaymentDO existing = paymentMapper.selectByPaymentNoIncludingDeleted(paymentNo);
            if (existing != null) {
                throw exception(RECEIPT_STATUS_NOT_ALLOW, "收款单号已存在");
            }
        } else {
            paymentNo = generateReceiptNo();
        }

        // 6. 落库主表
        BigDecimal prepayUsed = reqVO.getPrepayUsedAmount() != null ? reqVO.getPrepayUsedAmount() : BigDecimal.ZERO;
        PaymentDO payment = PaymentDO.builder()
                .paymentNo(paymentNo)
                .direction(DIRECTION_IN)
                .customerId(reqVO.getCustomerId())
                .accountId(reqVO.getAccountId())
                .payAmount(reqVO.getPayAmount())
                .prepayUsedAmount(prepayUsed)
                .status(STATUS_PENDING)
                .receiptType(reqVO.getReceiptType())
                .currency(reqVO.getCurrency().trim().toUpperCase())
                .exchangeRate(BigDecimal.ONE)
                .receiptDate(reqVO.getReceiptDate())
                .bankRefNo(reqVO.getBankRefNo())
                .handlerName(reqVO.getHandlerName())
                .approvalStatus(APPROVAL_STATUS_PENDING)
                .remark(reqVO.getRemark())
                .build();
        paymentMapper.insert(payment);

        // 7. 落库核销子表
        if (CollUtil.isNotEmpty(reqVO.getItems())) {
            for (ReceiptRefSaveVO item : reqVO.getItems()) {
                PaymentRefDO refDO = PaymentRefDO.builder()
                        .paymentId(payment.getId())
                        .refType(REF_TYPE_AR_ENTRY)
                        .refId(item.getLedgerEntryId())
                        .ledgerEntryId(item.getLedgerEntryId())
                        .entryNo(item.getEntryNo())
                        .bizDate(item.getBizDate())
                        .allocateAmount(item.getAllocateAmount())
                        .baseAmount(BigDecimal.ZERO)
                        .build();
                paymentRefMapper.insert(refDO);
            }
        }

        log.info("[createReceipt] 创建客户收款单成功: id={}, paymentNo={}", payment.getId(), payment.getPaymentNo());
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceipt(ReceiptSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(reqVO.getId());
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        if (!APPROVAL_STATUS_PENDING.equalsIgnoreCase(payment.getApprovalStatus())
                && !APPROVAL_STATUS_REJECTED.equalsIgnoreCase(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 校验资金账户
        AccountDO account = accountService.getAccount(reqVO.getAccountId());
        if (account == null || !"ENABLE".equalsIgnoreCase(account.getStatus())) {
            throw exception(ACCOUNT_DISABLED);
        }

        // 校验明细
        validateReceiptItems(reqVO.getCustomerId(), reqVO.getCurrency(), reqVO.getPayAmount(),
                reqVO.getPrepayUsedAmount(), reqVO.getItems(), payment.getId());

        // 更新主表
        BigDecimal prepayUsed = reqVO.getPrepayUsedAmount() != null ? reqVO.getPrepayUsedAmount() : BigDecimal.ZERO;
        payment.setCustomerId(reqVO.getCustomerId());
        payment.setAccountId(reqVO.getAccountId());
        payment.setPayAmount(reqVO.getPayAmount());
        payment.setPrepayUsedAmount(prepayUsed);
        payment.setReceiptType(reqVO.getReceiptType());
        payment.setCurrency(reqVO.getCurrency().trim().toUpperCase());
        payment.setReceiptDate(reqVO.getReceiptDate());
        payment.setBankRefNo(reqVO.getBankRefNo());
        payment.setHandlerName(reqVO.getHandlerName());
        payment.setRemark(reqVO.getRemark());
        paymentMapper.updateById(payment);

        // 重置核销行
        paymentRefMapper.delete(new LambdaQueryWrapper<PaymentRefDO>().eq(PaymentRefDO::getPaymentId, payment.getId()));
        if (CollUtil.isNotEmpty(reqVO.getItems())) {
            for (ReceiptRefSaveVO item : reqVO.getItems()) {
                PaymentRefDO refDO = PaymentRefDO.builder()
                        .paymentId(payment.getId())
                        .refType(REF_TYPE_AR_ENTRY)
                        .refId(item.getLedgerEntryId())
                        .ledgerEntryId(item.getLedgerEntryId())
                        .entryNo(item.getEntryNo())
                        .bizDate(item.getBizDate())
                        .allocateAmount(item.getAllocateAmount())
                        .baseAmount(BigDecimal.ZERO)
                        .build();
                paymentRefMapper.insert(refDO);
            }
        }

        log.info("[updateReceipt] 更新客户收款单成功: id={}", payment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceipt(Long id) {
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        if (!APPROVAL_STATUS_PENDING.equalsIgnoreCase(payment.getApprovalStatus())
                && !APPROVAL_STATUS_REJECTED.equalsIgnoreCase(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        paymentRefMapper.delete(new LambdaQueryWrapper<PaymentRefDO>().eq(PaymentRefDO::getPaymentId, id));
        paymentMapper.deleteById(id);
        log.info("[deleteReceipt] 删除客户收款单草稿成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "CUSTOMER_RECEIPT", subType = "SUBMIT", bizNo = "{{#id}}", success = "提交客户收款单审批【{{#id}}】")
    public Long submitReceipt(Long id) {
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        if (!APPROVAL_STATUS_PENDING.equalsIgnoreCase(payment.getApprovalStatus())
                && !APPROVAL_STATUS_REJECTED.equalsIgnoreCase(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 重新校验核销行余额
        List<PaymentRefDO> refs = paymentRefMapper.selectListByPaymentId(id);
        if (CollUtil.isNotEmpty(refs)) {
            List<ReceiptRefSaveVO> itemVOs = refs.stream().map(r -> ReceiptRefSaveVO.builder()
                    .ledgerEntryId(r.getLedgerEntryId())
                    .allocateAmount(r.getAllocateAmount())
                    .entryNo(r.getEntryNo())
                    .bizDate(r.getBizDate())
                    .build()).collect(Collectors.toList());
            validateReceiptItems(payment.getCustomerId(), payment.getCurrency(), payment.getPayAmount(),
                    payment.getPrepayUsedAmount(), itemVOs, id);
        }

        // 提交审批引擎
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType("CUSTOMER_RECEIPT");
        submitReqVO.setBizId(id);
        submitReqVO.setComment("客户收款单提交审批");
        Long instanceId = approvalInstanceService.submitInstance(submitReqVO);

        payment.setApprovalStatus(APPROVAL_STATUS_APPROVING);
        paymentMapper.updateById(payment);
        log.info("[submitReceipt] 客户收款单已提交审批: id={}, instanceId={}", id, instanceId);
        return instanceId;
    }

    @Override
    public AllocatePreviewRespVO allocatePreview(AllocatePreviewReqVO reqVO) {
        if (reqVO.getPayAmount() == null || reqVO.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIPT_ALLOCATE_EXCEED);
        }

        List<ArBalanceRespVO> arList = getArBalanceList(reqVO.getCustomerId(), reqVO.getCurrency());
        if (CollUtil.isNotEmpty(reqVO.getEntryIds())) {
            Set<Long> filterIds = new HashSet<>(reqVO.getEntryIds());
            arList = arList.stream().filter(ar -> filterIds.contains(ar.getLedgerEntryId())).collect(Collectors.toList());
        }

        // 按业务日期 FIFO 升序排序
        arList.sort(Comparator.comparing(ArBalanceRespVO::getBizDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ArBalanceRespVO::getLedgerEntryId));

        BigDecimal remaining = reqVO.getPayAmount();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<AllocateItemRespVO> allocatedItems = new ArrayList<>();

        for (ArBalanceRespVO ar : arList) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (ar.getUnallocatedBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal toAlloc = remaining.min(ar.getUnallocatedBalance());
            remaining = remaining.subtract(toAlloc);
            totalAllocated = totalAllocated.add(toAlloc);

            allocatedItems.add(AllocateItemRespVO.builder()
                    .ledgerEntryId(ar.getLedgerEntryId())
                    .entryNo(ar.getEntryNo())
                    .bizDate(ar.getBizDate())
                    .originalAmount(ar.getOriginalAmount())
                    .unallocatedBalance(ar.getUnallocatedBalance())
                    .allocatedAmount(toAlloc)
                    .refType(ar.getRefType())
                    .refId(ar.getRefId())
                    .build());
        }

        return AllocatePreviewRespVO.builder()
                .totalPayAmount(reqVO.getPayAmount())
                .totalAllocated(totalAllocated)
                .remainingPayAmount(remaining)
                .items(allocatedItems)
                .build();
    }

    @Override
    public List<ArBalanceRespVO> getArBalanceList(Long customerId, String currency) {
        if (customerId == null || StrUtil.isBlank(currency)) {
            return Collections.emptyList();
        }

        // 查询客户该币种下所有 AR_INC 分录
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                .eq(LedgerEntryDO::getPartyId, customerId)
                .eq(LedgerEntryDO::getCurrency, currency.trim().toUpperCase())
                .eq(LedgerEntryDO::getCategory, FinanceLedgerCategoryEnum.AR_INC.getValue())
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }

        List<ArBalanceRespVO> result = new ArrayList<>();
        for (LedgerEntryDO entry : entries) {
            BigDecimal unallocated = calculateUnallocatedBalance(entry, null);
            if (unallocated.compareTo(BigDecimal.ZERO) > 0) {
                String shipmentNo = null;
                String contractNo = null;
                if ("SHIPMENT".equalsIgnoreCase(entry.getRefType()) && entry.getRefId() != null) {
                    ShipmentDO shipment = shipmentMapper.selectById(entry.getRefId());
                    if (shipment != null) {
                        shipmentNo = shipment.getShipmentNo();
                        contractNo = shipment.getContractNo();
                    }
                }

                BigDecimal original = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
                BigDecimal reversals = calculateReversals(entry.getId());
                BigDecimal allocated = original.subtract(reversals).subtract(unallocated);

                result.add(ArBalanceRespVO.builder()
                        .ledgerEntryId(entry.getId())
                        .entryNo(StrUtil.isNotBlank(entry.getRemark()) ? entry.getRemark() : String.valueOf(entry.getId()))
                        .category(entry.getCategory())
                        .bizDate(entry.getBizDate())
                        .currency(entry.getCurrency())
                        .originalAmount(original)
                        .reversedAmount(reversals)
                        .allocatedAmount(allocated)
                        .unallocatedBalance(unallocated)
                        .refType(entry.getRefType())
                        .refId(entry.getRefId())
                        .shipmentNo(shipmentNo)
                        .contractNo(contractNo)
                        .build());
            }
        }

        return result;
    }

    @Override
    public PaymentRespVO getReceipt(Long id) {
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        CustomerDO customer = customerMapper.selectById(payment.getCustomerId());
        AccountDO account = accountService.getAccount(payment.getAccountId());

        List<PaymentRefDO> refs = paymentRefMapper.selectListByPaymentId(id);
        List<PaymentRefRespVO> itemVOs = new ArrayList<>();
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                itemVOs.add(PaymentRefRespVO.builder()
                        .id(ref.getId())
                        .paymentId(ref.getPaymentId())
                        .refType(ref.getRefType())
                        .refId(ref.getRefId())
                        .applyNo(ref.getApplyNo())
                        .entryNo(ref.getEntryNo())
                        .bizDate(ref.getBizDate())
                        .baseAmount(ref.getBaseAmount())
                        .ledgerEntryId(ref.getLedgerEntryId())
                        .allocateAmount(ref.getAllocateAmount())
                        .build());
            }
        }

        return PaymentRespVO.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .direction(payment.getDirection())
                .customerId(payment.getCustomerId())
                .customerName(customer != null ? customer.getCompanyName() : null)
                .accountId(payment.getAccountId())
                .accountName(account != null ? account.getAccountName() : null)
                .payAmount(payment.getPayAmount())
                .prepayUsedAmount(payment.getPrepayUsedAmount())
                .status(payment.getStatus())
                .receiptType(payment.getReceiptType())
                .currency(payment.getCurrency())
                .exchangeRate(payment.getExchangeRate())
                .receiptDate(payment.getReceiptDate())
                .bankRefNo(payment.getBankRefNo())
                .handlerName(payment.getHandlerName())
                .approvalStatus(payment.getApprovalStatus())
                .remark(payment.getRemark())
                .createTime(payment.getCreateTime())
                .items(itemVOs)
                .build();
    }

    @Override
    public PageResult<PaymentRespVO> getReceiptPage(PaymentPageReqVO pageReqVO) {
        LambdaQueryWrapperX<PaymentDO> queryWrapper = new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getDirection, DIRECTION_IN)
                .eqIfPresent(PaymentDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(PaymentDO::getAccountId, pageReqVO.getAccountId())
                .eqIfPresent(PaymentDO::getReceiptType, pageReqVO.getReceiptType())
                .eqIfPresent(PaymentDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .eqIfPresent(PaymentDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(PaymentDO::getCurrency, pageReqVO.getCurrency())
                .geIfPresent(PaymentDO::getReceiptDate, pageReqVO.getReceiptDateStart())
                .leIfPresent(PaymentDO::getReceiptDate, pageReqVO.getReceiptDateEnd())
                .geIfPresent(PaymentDO::getCreateTime, pageReqVO.getCreateTimeStart())
                .leIfPresent(PaymentDO::getCreateTime, pageReqVO.getCreateTimeEnd());

        if (StrUtil.isNotBlank(pageReqVO.getKeyword())) {
            String kw = pageReqVO.getKeyword().trim();
            queryWrapper.and(w -> w.like(PaymentDO::getPaymentNo, kw)
                    .or().like(PaymentDO::getHandlerName, kw)
                    .or().like(PaymentDO::getBankRefNo, kw));
        }

        queryWrapper.orderByDesc(PaymentDO::getId);
        PageResult<PaymentDO> pageResult = paymentMapper.selectPage(pageReqVO, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询客户与账户名称
        Set<Long> customerIds = pageResult.getList().stream().map(PaymentDO::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> accountIds = pageResult.getList().stream().map(PaymentDO::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, CustomerDO> customerMap = CollUtil.isNotEmpty(customerIds)
                ? customerMapper.selectBatchIds(customerIds).stream().collect(Collectors.toMap(CustomerDO::getId, Function.identity()))
                : Collections.emptyMap();

        Map<Long, AccountDO> accountMap = CollUtil.isNotEmpty(accountIds)
                ? accountService.getAccountList(accountIds).stream().collect(Collectors.toMap(AccountDO::getId, Function.identity()))
                : Collections.emptyMap();

        List<PaymentRespVO> voList = pageResult.getList().stream().map(p -> {
            CustomerDO c = customerMap.get(p.getCustomerId());
            AccountDO a = accountMap.get(p.getAccountId());
            return PaymentRespVO.builder()
                    .id(p.getId())
                    .paymentNo(p.getPaymentNo())
                    .direction(p.getDirection())
                    .customerId(p.getCustomerId())
                    .customerName(c != null ? c.getCompanyName() : null)
                    .accountId(p.getAccountId())
                    .accountName(a != null ? a.getAccountName() : null)
                    .payAmount(p.getPayAmount())
                    .prepayUsedAmount(p.getPrepayUsedAmount())
                    .status(p.getStatus())
                    .receiptType(p.getReceiptType())
                    .currency(p.getCurrency())
                    .exchangeRate(p.getExchangeRate())
                    .receiptDate(p.getReceiptDate())
                    .bankRefNo(p.getBankRefNo())
                    .handlerName(p.getHandlerName())
                    .approvalStatus(p.getApprovalStatus())
                    .remark(p.getRemark())
                    .createTime(p.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postReceiptOnApproval(Long paymentId) {
        PaymentDO payment = paymentMapper.selectById(paymentId);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            log.warn("[postReceiptOnApproval] 收款单不存在或非IN收款单: id={}", paymentId);
            return;
        }

        if (STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus())) {
            log.warn("[postReceiptOnApproval] 收款单已过账，忽略重复过账: id={}", paymentId);
            return;
        }

        // 1. 获取即期汇率
        BigDecimal rate = exchangeRateService.getRate(payment.getCurrency(), payment.getReceiptDate());

        // 2. 校验核销行余额（防并发超核销，加行级排他锁）
        List<PaymentRefDO> refs = paymentRefMapper.selectListByPaymentId(paymentId);
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                LedgerEntryDO entry = ledgerEntryMapper.selectOne(
                        new LambdaQueryWrapperX<LedgerEntryDO>()
                                .eq(LedgerEntryDO::getId, ref.getLedgerEntryId())
                                .last("FOR UPDATE")
                );
                if (entry == null) {
                    entry = ledgerEntryMapper.selectById(ref.getLedgerEntryId());
                }
                if (entry == null) {
                    throw exception(RECEIPT_POST_CONFLICT, "核销目标分录不存在: id=" + ref.getLedgerEntryId());
                }
                BigDecimal unallocated = calculateUnallocatedBalance(entry, paymentId);
                if (unallocated.compareTo(ref.getAllocateAmount()) < 0) {
                    throw exception(RECEIPT_POST_CONFLICT, "分录【" + ref.getEntryNo() + "】可核销余额不足，发生并发冲突");
                }
            }
        }

        // 3. 资金账户入账（current_balance += payAmount）
        accountService.updateBalance(payment.getAccountId(), payment.getPayAmount());

        // 4. 经营分录入账（收款入账，贷记客户往来）
        String cat = FinanceLedgerCategoryEnum.RECEIPT.getValue();
        BigDecimal baseAmount = payment.getPayAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        LedgerEntryDO ledgerEntry = LedgerEntryDO.builder()
                .bizDate(payment.getReceiptDate())
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(payment.getCustomerId())
                .accountId(payment.getAccountId())
                .category(cat)
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .currency(payment.getCurrency())
                .exchangeRate(rate)
                .originalAmount(payment.getPayAmount())
                .baseAmount(baseAmount)
                .refType(FinanceLedgerRefTypeEnum.PAYMENT.getValue())
                .refId(payment.getId())
                .remark("客户收款入账 - " + payment.getPaymentNo())
                .build();
        Long savedEntryId = ledgerService.append(ledgerEntry);

        // 5. 更新核销行折算本位币金额
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                BigDecimal refBaseAmt = ref.getAllocateAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
                ref.setBaseAmount(refBaseAmt);
                paymentRefMapper.updateById(ref);
            }
        }

        // 6. 更新收款单状态
        payment.setStatus(STATUS_COMPLETED);
        payment.setApprovalStatus(APPROVAL_STATUS_APPROVED);
        payment.setExchangeRate(rate);
        paymentMapper.updateById(payment);

        log.info("[postReceiptOnApproval] 客户收款单同事务四写过账成功: id={}, paymentNo={}, baseAmount={}",
                payment.getId(), payment.getPaymentNo(), baseAmount);
    }

    private void validateReceiptItems(Long customerId, String currency, BigDecimal payAmount,
                                      BigDecimal prepayUsedAmount, List<ReceiptRefSaveVO> items,
                                      Long excludePaymentId) {
        if (CollUtil.isEmpty(items)) {
            return;
        }

        BigDecimal prepay = prepayUsedAmount != null ? prepayUsedAmount : BigDecimal.ZERO;
        BigDecimal totalAvailable = payAmount.add(prepay);
        BigDecimal sumAllocated = BigDecimal.ZERO;

        for (ReceiptRefSaveVO item : items) {
            if (item.getAllocateAmount() == null || item.getAllocateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(RECEIPT_ALLOCATE_EXCEED, "分配金额必须大于0");
            }
            sumAllocated = sumAllocated.add(item.getAllocateAmount());

            LedgerEntryDO entry = ledgerEntryMapper.selectById(item.getLedgerEntryId());
            if (entry == null || !FinancePartyTypeEnum.CUSTOMER.getValue().equalsIgnoreCase(entry.getPartyType())
                    || !customerId.equals(entry.getPartyId())) {
                throw exception(RECEIPT_CUSTOMER_MISMATCH);
            }

            if (!currency.trim().equalsIgnoreCase(entry.getCurrency())) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }

            BigDecimal unallocated = calculateUnallocatedBalance(entry, excludePaymentId);
            if (item.getAllocateAmount().compareTo(unallocated) > 0) {
                throw exception(RECEIPT_ALLOCATE_EXCEED, "分录【" + item.getEntryNo() + "】分配金额超过未核销余额");
            }
        }

        if (sumAllocated.compareTo(totalAvailable) > 0) {
            throw exception(RECEIPT_ALLOCATE_EXCEED, "分配合计不能超过收款总额");
        }
    }

    private BigDecimal calculateUnallocatedBalance(LedgerEntryDO entry, Long excludePaymentId) {
        BigDecimal original = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
        BigDecimal reversals = calculateReversals(entry.getId());
        BigDecimal writeoffs = calculateWriteoffs(entry.getId(), excludePaymentId);
        BigDecimal unallocated = original.subtract(reversals).subtract(writeoffs);
        return unallocated.compareTo(BigDecimal.ZERO) > 0 ? unallocated : BigDecimal.ZERO;
    }

    private BigDecimal calculateReversals(Long ledgerEntryId) {
        List<LedgerEntryDO> revEntries = ledgerEntryMapper.selectListByReversalOfId(ledgerEntryId);
        BigDecimal sum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(revEntries)) {
            for (LedgerEntryDO rev : revEntries) {
                if (FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue().equalsIgnoreCase(rev.getCategory())
                        || FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue().equalsIgnoreCase(rev.getCategory())) {
                    sum = sum.add(rev.getOriginalAmount() != null ? rev.getOriginalAmount() : BigDecimal.ZERO);
                }
            }
        }
        return sum;
    }

    private BigDecimal calculateWriteoffs(Long ledgerEntryId, Long excludePaymentId) {
        List<PaymentRefDO> refs = paymentRefMapper.selectListByRefTypeAndRefId(REF_TYPE_AR_ENTRY, ledgerEntryId);
        if (CollUtil.isEmpty(refs)) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentRefDO ref : refs) {
            if (excludePaymentId != null && excludePaymentId.equals(ref.getPaymentId())) {
                continue;
            }
            PaymentDO payment = paymentMapper.selectById(ref.getPaymentId());
            if (payment != null && STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus())) {
                sum = sum.add(ref.getAllocateAmount() != null ? ref.getAllocateAmount() : BigDecimal.ZERO);
            }
        }
        return sum;
    }

    private String generateReceiptNo() {
        LocalDate today = LocalDate.now();
        String prefix = RECEIPT_NO_PREFIX + today.format(DATE_FORMATTER) + "-";
        List<PaymentDO> candidates = paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(prefix);

        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        if (CollUtil.isNotEmpty(candidates)) {
            for (PaymentDO candidate : candidates) {
                String no = candidate.getPaymentNo();
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
                    long val = Long.parseLong(digits);
                    if (val > maxNum) {
                        maxNum = val;
                    }
                    if (digits.length() > maxDigits) {
                        maxDigits = digits.length();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int retry = 0; retry < MAX_GENERATE_RETRY; retry++) {
            long nextSeq = maxNum + 1 + retry;
            String seqStr = String.valueOf(nextSeq);
            if (seqStr.length() < maxDigits) {
                seqStr = String.format("%0" + maxDigits + "d", nextSeq);
            }
            String candidateNo = prefix + seqStr;
            PaymentDO existing = paymentMapper.selectByPaymentNoIncludingDeleted(candidateNo);
            if (existing == null) {
                return candidateNo;
            }
        }

        return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseReceipt(Long id) {
        if (id == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        // 状态检查：必须处于已完成 COMPLETED 状态；REVERSED 状态不可重复红冲（幂等硬拦截）
        if (!STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus())) {
            throw exception(RECEIPT_REVERSAL_NOT_ALLOW, "收款单非已完成状态，不允许红冲");
        }

        // 若为预收款单，检查是否已经发生预收转核销
        if (RECEIPT_TYPE_ADVANCE.equalsIgnoreCase(payment.getReceiptType())) {
            List<PaymentRefDO> existingRefs = paymentRefMapper.selectListByPaymentId(payment.getId());
            if (CollUtil.isNotEmpty(existingRefs)) {
                BigDecimal usedAdvance = existingRefs.stream()
                        .map(r -> r.getAllocateAmount() != null ? r.getAllocateAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (usedAdvance.compareTo(BigDecimal.ZERO) > 0) {
                    throw exception(RECEIPT_REVERSAL_NOT_ALLOW, "该预收款单已发生预收转核销，不允许红冲");
                }
            }
        }

        // 1. 资金账户回扣（current_balance -= payAmount）
        accountService.updateBalance(payment.getAccountId(), payment.getPayAmount().negate());

        // 2. 查询原入账分录（回指）
        Long origEntryId = null;
        List<LedgerEntryDO> origEntries = ledgerEntryMapper.selectListByRef(
                FinanceLedgerRefTypeEnum.PAYMENT.getValue(), payment.getId());
        if (CollUtil.isNotEmpty(origEntries)) {
            origEntryId = origEntries.get(0).getId();
        }

        // 3. 生成 RECEIPT_REVERSAL 反向分录（借记客户往来，恢复应收）
        BigDecimal rate = payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE;
        BigDecimal baseAmount = payment.getPayAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        LedgerEntryDO reversalEntry = LedgerEntryDO.builder()
                .bizDate(LocalDate.now())
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(payment.getCustomerId())
                .accountId(payment.getAccountId())
                .category(FinanceLedgerCategoryEnum.RECEIPT_REVERSAL.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency(payment.getCurrency())
                .exchangeRate(rate)
                .originalAmount(payment.getPayAmount())
                .baseAmount(baseAmount)
                .refType(FinanceLedgerRefTypeEnum.PAYMENT.getValue())
                .refId(payment.getId())
                .reversalOfId(origEntryId)
                .remark("客户收款单红冲 - " + payment.getPaymentNo())
                .build();
        ledgerService.append(reversalEntry);

        // 4. 更新收款单状态为 REVERSED（核销行全部自动重开，未核销余额自动恢复）
        payment.setStatus(STATUS_REVERSED);
        paymentMapper.updateById(payment);

        log.info("[reverseReceipt] 客户收款单红冲成功: id={}, paymentNo={}, deductedAmount={}",
                payment.getId(), payment.getPaymentNo(), payment.getPayAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceWriteoff(AdvanceWriteoffReqVO reqVO) {
        if (reqVO.getPaymentId() == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(reqVO.getPaymentId());
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        if (!STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW, "收款单非已过账状态，无法进行预收转核销");
        }

        if (!RECEIPT_TYPE_ADVANCE.equalsIgnoreCase(payment.getReceiptType())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW, "仅预收款单允许进行预收转核销");
        }

        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(RECEIPT_ALLOCATE_EXCEED, "核销分录列表不能为空");
        }

        // 1. 检查可用预收余额
        BigDecimal advanceBalance = getAdvanceBalance(payment.getId());
        BigDecimal sumAllocated = BigDecimal.ZERO;
        for (ReceiptRefSaveVO item : reqVO.getItems()) {
            if (item.getAllocateAmount() == null || item.getAllocateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(RECEIPT_ALLOCATE_EXCEED, "分配金额必须大于0");
            }
            sumAllocated = sumAllocated.add(item.getAllocateAmount());
        }

        if (sumAllocated.compareTo(advanceBalance) > 0) {
            throw exception(RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH,
                    "核销总额【" + sumAllocated + "】超出可用预收余额【" + advanceBalance + "】");
        }

        // 2. 逐行校验分录客户、币种，并执行并发行级排他锁和余额防超核销检查
        BigDecimal rate = exchangeRateService.getRate(payment.getCurrency(), LocalDate.now());

        for (ReceiptRefSaveVO item : reqVO.getItems()) {
            // 加排他行锁防并发超核销
            LedgerEntryDO entry = ledgerEntryMapper.selectOne(
                    new LambdaQueryWrapperX<LedgerEntryDO>()
                            .eq(LedgerEntryDO::getId, item.getLedgerEntryId())
                            .last("FOR UPDATE")
            );
            if (entry == null) {
                entry = ledgerEntryMapper.selectById(item.getLedgerEntryId());
            }
            if (entry == null || !FinancePartyTypeEnum.CUSTOMER.getValue().equalsIgnoreCase(entry.getPartyType())
                    || !payment.getCustomerId().equals(entry.getPartyId())) {
                throw exception(RECEIPT_CUSTOMER_MISMATCH);
            }

            if (!payment.getCurrency().trim().equalsIgnoreCase(entry.getCurrency())) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }

            BigDecimal unallocated = calculateUnallocatedBalance(entry, null);
            String entryLabel = StrUtil.isNotBlank(item.getEntryNo()) ? item.getEntryNo() : String.valueOf(entry.getId());
            if (item.getAllocateAmount().compareTo(unallocated) > 0) {
                throw exception(RECEIPT_POST_CONFLICT,
                        "分录【" + entryLabel + "】可核销余额不足，发生并发冲突");
            }

            BigDecimal baseAmount = item.getAllocateAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            PaymentRefDO refDO = PaymentRefDO.builder()
                    .paymentId(payment.getId())
                    .refType(REF_TYPE_AR_ENTRY)
                    .refId(item.getLedgerEntryId())
                    .ledgerEntryId(item.getLedgerEntryId())
                    .entryNo(entryLabel)
                    .bizDate(item.getBizDate() != null ? item.getBizDate() : entry.getBizDate())
                    .allocateAmount(item.getAllocateAmount())
                    .baseAmount(baseAmount)
                    .build();
            paymentRefMapper.insert(refDO);
        }

        // 3. 生成 ADVANCE_WRITE_OFF 经营分录（贷记客户往来，冲减应收）
        BigDecimal totalBaseAmount = sumAllocated.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        LedgerEntryDO writeoffEntry = LedgerEntryDO.builder()
                .bizDate(LocalDate.now())
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(payment.getCustomerId())
                .accountId(payment.getAccountId())
                .category(FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue())
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .currency(payment.getCurrency())
                .exchangeRate(rate)
                .originalAmount(sumAllocated)
                .baseAmount(totalBaseAmount)
                .refType(FinanceLedgerRefTypeEnum.PAYMENT.getValue())
                .refId(payment.getId())
                .remark(StrUtil.isNotBlank(reqVO.getRemark()) ? reqVO.getRemark() : ("客户预收转核销 - " + payment.getPaymentNo()))
                .build();
        ledgerService.append(writeoffEntry);

        // 4. 更新原预收款单的已核销预收金额记录
        BigDecimal currentPrepayUsed = payment.getPrepayUsedAmount() != null ? payment.getPrepayUsedAmount() : BigDecimal.ZERO;
        payment.setPrepayUsedAmount(currentPrepayUsed.add(sumAllocated));
        paymentMapper.updateById(payment);

        log.info("[advanceWriteoff] 预收转核销成功: paymentId={}, paymentNo={}, sumAllocated={}",
                payment.getId(), payment.getPaymentNo(), sumAllocated);
    }

    @Override
    public BigDecimal getAdvanceBalance(Long paymentId) {
        if (paymentId == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(paymentId);
        if (payment == null || !DIRECTION_IN.equals(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        if (!RECEIPT_TYPE_ADVANCE.equalsIgnoreCase(payment.getReceiptType())) {
            return BigDecimal.ZERO;
        }

        BigDecimal payAmount = payment.getPayAmount() != null ? payment.getPayAmount() : BigDecimal.ZERO;
        List<PaymentRefDO> refs = paymentRefMapper.selectListByPaymentId(paymentId);
        BigDecimal used = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                if (ref.getAllocateAmount() != null) {
                    used = used.add(ref.getAllocateAmount());
                }
            }
        }
        BigDecimal balance = payAmount.subtract(used);
        return balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO;
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.Direction;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentRefType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReceiptType;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinanceAccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 收付款（OUT 侧付款登记）Service 实现类
 *
 * <p>冲突点代码注释挂账共享件清单：
 * 1. payment_no 与 10 号 IN 侧共享 PAY 前缀，收付款单统一按天取号（PAY+YYYYMMDD+3位流水），表结构合并时收口；
 * 2. lunaerp_finance_payment 与 lunaerp_finance_payment_ref 为共享表，OUT 侧 direction='OUT'，ref_type='PAY_APPLY'；
 * 3. 资金账户服务：调用 finance.account 服务条件更新扣减 pay_amount，未落地前由 FinanceAccountService seam 表达；
 * 4. 经营分录账：付款登记追加一条 OUT_SUPPLIER 分录，金额为 Σ(pay_amount + prepay_used)，与 10/12 号共用唯一 append-only 账本。
 * </p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final String RECEIPT_NO_PREFIX = "RC-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_RETRY_COUNT = 5;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+$");

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private PaymentApplyService paymentApplyService;

    @Resource
    private FinanceAccountService financeAccountService;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Resource
    private AccountService accountService;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_PAYMENT", subType = "REGISTER_PAYMENT", bizNo = "{{#_ret}}", success = "登记付款单【{{#_ret}}】成功")
    public Long registerPayment(PaymentRegisterReqVO reqVO) {
        // 1. 入参校验
        if (reqVO == null || reqVO.getSupplierId() == null || reqVO.getAccountId() == null
                || CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        BigDecimal payAmount = reqVO.getPayAmount() != null ? reqVO.getPayAmount() : BigDecimal.ZERO;
        BigDecimal prepayUsed = reqVO.getPrepayUsedAmount() != null ? reqVO.getPrepayUsedAmount() : BigDecimal.ZERO;

        if (payAmount.compareTo(BigDecimal.ZERO) < 0 || prepayUsed.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }
        if (payAmount.add(prepayUsed).compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        Long currentTenantId = TenantContextHolder.getTenantId();

        // 2. 预付守卫：prepay_used_amount ≤ 供应商预付余额（023_005）
        if (prepayUsed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal prepayBalance = paymentApplyService.getPrepayBalance(reqVO.getSupplierId());
            if (prepayBalance == null) {
                prepayBalance = BigDecimal.ZERO;
            }
            if (prepayUsed.compareTo(prepayBalance) > 0) {
                throw exception(PAYMENT_PREPAY_BALANCE_NOT_ENOUGH);
            }
        }

        // 3. 账户守卫：账户存在且未停用（023_006）
        financeAccountService.validateAccountAvailable(reqVO.getAccountId());

        // 4. 核销申请校验与分配计算
        List<AllocatedItem> allocatedItems = calculateAllocations(reqVO, payAmount, prepayUsed, currentTenantId);

        // 5. 账户扣款：经 finance.account 服务条件更新扣减 pay_amount，余额不足/更新失败报 023_004
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeAccountService.debitAccount(reqVO.getAccountId(), payAmount);
        }

        // 6. 同事务四步原子：
        // 6.1 写 payment(direction=OUT, status=COMPLETED)
        String paymentNo = generatePaymentNo(LocalDate.now());
        PaymentDO payment = new PaymentDO();
        payment.setPaymentNo(paymentNo);
        payment.setDirection(Direction.OUT.getValue());
        payment.setSupplierId(reqVO.getSupplierId());
        payment.setAccountId(reqVO.getAccountId());
        payment.setPayAmount(payAmount.setScale(2, RoundingMode.HALF_UP));
        payment.setPrepayUsedAmount(prepayUsed.setScale(2, RoundingMode.HALF_UP));
        payment.setStatus(PaymentStatus.COMPLETED.getValue());
        payment.setRemark(reqVO.getRemark());
        paymentMapper.insert(payment);

        // 6.2 每申请一条 payment_ref（apply_no 快照）
        for (AllocatedItem item : allocatedItems) {
            PaymentRefDO ref = new PaymentRefDO();
            ref.setPaymentId(payment.getId());
            ref.setRefType(PaymentRefType.PAY_APPLY.getValue());
            ref.setRefId(item.getApply().getId());
            ref.setApplyNo(item.getApply().getApplyNo());
            ref.setAllocateAmount(item.getAllocateAmount().setScale(2, RoundingMode.HALF_UP));
            paymentRefMapper.insert(ref);
        }

        // 6.3 调账户服务写流出流水 pay_amount
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeAccountService.recordAccountOutflow(reqVO.getAccountId(), payAmount, payment.getId(), payment.getPaymentNo());
        }

        // 6.4 追加一条 OUT_SUPPLIER 经营分录（金额 Σ(pay_amount+prepay_used)，refType=PAYMENT/refId=payment.id，备注含 prepay_used）
        BigDecimal totalLedgerAmount = payAmount.add(prepayUsed).setScale(2, RoundingMode.HALF_UP);
        String ledgerRemark = "供应商付款: 实付" + payAmount.toPlainString() + "元, 预付抵扣" + prepayUsed.toPlainString() + "元"
                + (StrUtil.isNotBlank(reqVO.getRemark()) ? (" (" + reqVO.getRemark() + ")") : "");
        financeAccountService.appendOutSupplierLedgerEntry(payment.getId(), payment.getPaymentNo(), reqVO.getSupplierId(),
                totalLedgerAmount, prepayUsed, reqVO.getAccountId(), ledgerRemark);

        log.info("[registerPayment] 付款登记成功: paymentId={}, paymentNo={}, payAmount={}, prepayUsed={}",
                payment.getId(), payment.getPaymentNo(), payAmount, prepayUsed);
        return payment.getId();
    }

    @Override
    public PaymentRespVO getPayment(Long id) {
        if (id == null) {
            throw exception(PAYMENT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw exception(PAYMENT_NOT_EXISTS);
        }
        validateTenant(payment.getTenantId());

        PaymentRespVO vo = BeanUtils.toBean(payment, PaymentRespVO.class);
        if (payment.getSupplierId() != null) {
            SupplierDO supplier = supplierMapper.selectById(payment.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getCompanyName());
            }
        }

        List<PaymentRefDO> refs = paymentRefMapper.selectList(
                new LambdaQueryWrapperX<PaymentRefDO>()
                        .eq(PaymentRefDO::getPaymentId, id)
                        .orderByAsc(PaymentRefDO::getId));
        vo.setItems(BeanUtils.toBean(refs, PaymentRefRespVO.class));
        return vo;
    }

    @Override
    public PageResult<PaymentRespVO> getPaymentPage(PaymentPageReqVO pageReqVO) {
        LambdaQueryWrapperX<PaymentDO> queryWrapper = new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getDirection, Direction.OUT.getValue())
                .eqIfPresent(PaymentDO::getSupplierId, pageReqVO.getSupplierId())
                .eqIfPresent(PaymentDO::getAccountId, pageReqVO.getAccountId())
                .betweenIfPresent(PaymentDO::getCreateTime, pageReqVO.getCreateTime())
                .and(StrUtil.isNotBlank(pageReqVO.getKeyword()), w -> w
                        .like(PaymentDO::getPaymentNo, pageReqVO.getKeyword())
                        .or().like(PaymentDO::getRemark, pageReqVO.getKeyword()))
                .orderByDesc(PaymentDO::getId);

        PageResult<PaymentDO> pageResult = paymentMapper.selectPage(pageReqVO, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // § 2 防 N+1 批量快照模式：批量加载供应商名称
        Set<Long> supplierIds = CollectionUtils.convertSet(pageResult.getList(), PaymentDO::getSupplierId);
        Map<Long, String> supplierNameMap = batchLoadSupplierNameMap(supplierIds);

        List<PaymentRespVO> respList = new ArrayList<>(pageResult.getList().size());
        for (PaymentDO payment : pageResult.getList()) {
            PaymentRespVO vo = BeanUtils.toBean(payment, PaymentRespVO.class);
            vo.setSupplierName(supplierNameMap.get(payment.getSupplierId()));
            respList.add(vo);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    // ==================== 私有辅助方法 ====================

    private List<AllocatedItem> calculateAllocations(PaymentRegisterReqVO reqVO, BigDecimal payAmount,
                                                    BigDecimal prepayUsed, Long currentTenantId) {
        // 1. 检查重复 apply_id 与混合传参
        Set<Long> applyIdSet = new HashSet<>();
        boolean hasNullAlloc = false;
        boolean hasNonNullAlloc = false;
        for (PaymentRegisterItemReqVO item : reqVO.getItems()) {
            if (item.getApplyId() == null) {
                throw exception(PAYMENT_APPLY_NOT_EXISTS);
            }
            if (!applyIdSet.add(item.getApplyId())) {
                throw exception(PAYMENT_AMOUNT_INVALID); // 重复 apply_id 拦截，防突破限额
            }
            if (item.getAllocateAmount() == null) {
                hasNullAlloc = true;
            } else {
                hasNonNullAlloc = true;
            }
        }
        if (hasNullAlloc && hasNonNullAlloc) {
            throw exception(PAYMENT_AMOUNT_INVALID); // 严禁混合部分为 null 部分传值
        }
        boolean isAutoAllocate = hasNullAlloc;

        // 2. § 13 来源行悲观锁串行化：按 applyId 顺序加排他锁防并发幻读与超申
        List<Long> sortedApplyIds = new ArrayList<>(applyIdSet);
        Collections.sort(sortedApplyIds);

        Map<Long, PaymentApplyDO> applyMap = new HashMap<>(sortedApplyIds.size());
        for (Long applyId : sortedApplyIds) {
            PaymentApplyDO apply = paymentApplyMapper.selectOneForUpdate(PaymentApplyDO::getId, applyId);
            if (apply == null) {
                apply = paymentApplyMapper.selectById(applyId);
            }
            if (apply == null) {
                throw exception(PAYMENT_APPLY_NOT_EXISTS);
            }
            if (currentTenantId != null && apply.getTenantId() != null && !currentTenantId.equals(apply.getTenantId())) {
                throw exception(PAYMENT_APPLY_NOT_EXISTS);
            }

            // 守卫：各申请属于同一供应商（否则 023_007）
            if (!reqVO.getSupplierId().equals(apply.getSupplierId())) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }

            // 守卫：各申请须 APPROVED（否则 023_001）
            if (!ApplyStatus.APPROVED.getValue().equalsIgnoreCase(apply.getApprovalStatus())) {
                throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
            }

            applyMap.put(applyId, apply);
        }

        // 3. § 2 防 N+1 批量快照模式：批量查询已核销金额
        Map<Long, BigDecimal> allocatedMap = batchLoadAllocatedMap(sortedApplyIds);

        Map<Long, ApplyContext> applyContextMap = new HashMap<>(sortedApplyIds.size());
        for (Long applyId : sortedApplyIds) {
            PaymentApplyDO apply = applyMap.get(applyId);
            BigDecimal allocated = allocatedMap.get(applyId);
            if (allocated == null) {
                allocated = paymentRefMapper.selectSumAllocatedAmountByApplyId(applyId);
            }
            if (allocated == null) {
                allocated = BigDecimal.ZERO;
            }
            BigDecimal remain = apply.getAmount().subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PAYMENT_ALLOCATE_EXCEED);
            }
            applyContextMap.put(applyId, new ApplyContext(apply, remain));
        }

        // 4. 分配计算
        List<AllocatedItem> result = new ArrayList<>();
        if (isAutoAllocate) {
            // 自动分配：按申请创建时间升序
            List<ApplyContext> sortedContexts = new ArrayList<>(applyContextMap.values());
            sortedContexts.sort(Comparator.comparing(ApplyContext::getApply, applyCreateTimeComparator()));

            BigDecimal pool = payAmount.add(prepayUsed);
            for (ApplyContext ctx : sortedContexts) {
                if (pool.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal alloc = pool.min(ctx.getRemainAmount());
                if (alloc.compareTo(BigDecimal.ZERO) > 0) {
                    result.add(new AllocatedItem(ctx.getApply(), alloc));
                    pool = pool.subtract(alloc);
                }
            }
            if (result.isEmpty()) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }
        } else {
            // 手工指定分配额校验
            BigDecimal sumAllocated = BigDecimal.ZERO;
            List<AllocatedItem> tempItems = new ArrayList<>();
            for (PaymentRegisterItemReqVO item : reqVO.getItems()) {
                BigDecimal alloc = item.getAllocateAmount();
                if (alloc == null || alloc.compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PAYMENT_AMOUNT_INVALID);
                }
                ApplyContext ctx = applyContextMap.get(item.getApplyId());
                if (alloc.compareTo(ctx.getRemainAmount()) > 0) {
                    throw exception(PAYMENT_ALLOCATE_EXCEED);
                }
                sumAllocated = sumAllocated.add(alloc);
                tempItems.add(new AllocatedItem(ctx.getApply(), alloc));
            }

            // Σ allocate_amount ≤ pay_amount + prepay_used_amount，钱不够分报 023_007
            if (sumAllocated.compareTo(payAmount.add(prepayUsed)) > 0) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }

            // 按申请创建顺序排列
            tempItems.sort(Comparator.comparing(AllocatedItem::getApply, applyCreateTimeComparator()));
            result = tempItems;
        }

        return result;
    }

    private Map<Long, BigDecimal> batchLoadAllocatedMap(Collection<Long> applyIds) {
        if (CollUtil.isEmpty(applyIds)) {
            return Collections.emptyMap();
        }
        List<PaymentRefDO> refs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getRefType, PaymentRefType.PAY_APPLY.getValue())
                .inOrEmpty(PaymentRefDO::getRefId, applyIds));
        Map<Long, BigDecimal> map = new HashMap<>();
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                if (ref.getRefId() != null && ref.getAllocateAmount() != null) {
                    map.merge(ref.getRefId(), ref.getAllocateAmount(), BigDecimal::add);
                }
            }
        }
        return map;
    }

    private Map<Long, String> batchLoadSupplierNameMap(Collection<Long> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return Collections.emptyMap();
        }
        List<SupplierDO> suppliers = supplierMapper.selectBatchIds(supplierIds);
        if (CollUtil.isEmpty(suppliers)) {
            return Collections.emptyMap();
        }
        return suppliers.stream().collect(
                Collectors.toMap(SupplierDO::getId, SupplierDO::getCompanyName, (a, b) -> a));
    }

    private static Comparator<PaymentApplyDO> applyCreateTimeComparator() {
        return Comparator
                .comparing((PaymentApplyDO a) -> a.getCreateTime() != null ? a.getCreateTime() : LocalDateTime.MIN)
                .thenComparing(PaymentApplyDO::getId);
    }

    private String generatePaymentNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = PAYMENT_NO_PREFIX + DATE_FORMATTER.format(targetDate);
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
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            long nextNum = maxNum + 1 + i;
            String paymentNo = String.format("%s%0" + maxDigits + "d", prefix, nextNum);
            PaymentDO existing = paymentMapper.selectByPaymentNoIncludingDeleted(paymentNo);
            if (existing == null) {
                return paymentNo;
            }
        }
        log.error("[generatePaymentNo] 收付款单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(PAYMENT_NO_EXHAUSTED);
    }

    private void validateTenant(Long tenantId) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && tenantId != null && !currentTenantId.equals(tenantId)) {
            throw exception(PAYMENT_NOT_EXISTS);
        }
    }

    @Data
    @AllArgsConstructor
    private static class ApplyContext {
        private PaymentApplyDO apply;
        private BigDecimal remainAmount;
    }

    @Data
    @AllArgsConstructor
    private static class AllocatedItem {
        private PaymentApplyDO apply;
        private BigDecimal allocateAmount;
    }

    // ==================== 收款单（IN 侧）业务实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_PAYMENT", subType = "CREATE_RECEIPT", bizNo = "{{#_ret}}", success = "创建收款单【{{#_ret}}】成功")
    public Long createReceipt(ReceiptSaveReqVO createReqVO) {
        if (createReqVO == null || createReqVO.getCustomerId() == null || createReqVO.getAccountId() == null
                || createReqVO.getPayAmount() == null) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        // 1. 校验客户存在
        CustomerDO customer = customerMapper.selectById(createReqVO.getCustomerId());
        if (customer == null) {
            throw exception(RECEIPT_CUSTOMER_MISMATCH);
        }
        validateTenant(customer.getTenantId());

        // 2. 校验资金账户存在且为 ENABLE
        AccountDO account = accountMapper.selectById(createReqVO.getAccountId());
        if (account == null || !"ENABLE".equalsIgnoreCase(account.getStatus())) {
            throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
        }
        validateTenant(account.getTenantId());
        if (StrUtil.isNotBlank(account.getCurrency()) && StrUtil.isNotBlank(createReqVO.getCurrency())
                && !account.getCurrency().equalsIgnoreCase(createReqVO.getCurrency())) {
            throw exception(RECEIPT_CURRENCY_MISMATCH);
        }

        // 3. 校验核销明细（如有）
        validateReceiptItems(createReqVO.getCurrency(), createReqVO.getCustomerId(),
                createReqVO.getPayAmount(), createReqVO.getItems());

        // 4. ADR-0004 单号生成（RC-{yyyyMMdd}-{3位日流水}）
        String paymentNo = StrUtil.isNotBlank(createReqVO.getPaymentNo())
                ? createReqVO.getPaymentNo().trim()
                : generateReceiptNo(createReqVO.getReceiptDate());
        PaymentDO existing = paymentMapper.selectByPaymentNoIncludingDeleted(paymentNo);
        if (existing != null) {
            throw exception(PAYMENT_NO_EXHAUSTED);
        }

        // 5. 保存收款单主表（PENDING 草稿）
        PaymentDO payment = new PaymentDO();
        payment.setPaymentNo(paymentNo);
        payment.setDirection("IN");
        payment.setCustomerId(customer.getId());
        payment.setAccountId(account.getId());
        payment.setCurrency(createReqVO.getCurrency());
        payment.setReceiptType(createReqVO.getReceiptType());
        payment.setPayAmount(createReqVO.getPayAmount().setScale(2, RoundingMode.HALF_UP));
        payment.setPrepayUsedAmount(BigDecimal.ZERO);
        payment.setReceiptDate(createReqVO.getReceiptDate() != null ? createReqVO.getReceiptDate() : LocalDate.now());
        payment.setBankRefNo(createReqVO.getBankRefNo());
        payment.setHandlerName(createReqVO.getHandlerName());
        payment.setApprovalStatus(ApplyStatus.PENDING.getValue());
        payment.setStatus("PENDING");
        payment.setRemark(createReqVO.getRemark());

        paymentMapper.insert(payment);

        // 6. 保存拟核销分配行
        saveReceiptRefs(payment.getId(), createReqVO.getItems());

        log.info("[createReceipt] 创建收款单草稿成功: id={}, paymentNo={}", payment.getId(), payment.getPaymentNo());
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceipt(ReceiptUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        PaymentDO payment = paymentMapper.selectById(updateReqVO.getId());
        validateReceiptExistsAndTenant(payment);

        // 仅 PENDING 草稿可修改（对齐 Spec 最小契约原则）
        if (!isDraft(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 校验资金账户可用
        if (updateReqVO.getAccountId() != null) {
            AccountDO account = accountMapper.selectById(updateReqVO.getAccountId());
            if (account == null || !"ENABLE".equalsIgnoreCase(account.getStatus())) {
                throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
            }
            if (StrUtil.isNotBlank(account.getCurrency()) && StrUtil.isNotBlank(payment.getCurrency())
                    && !account.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }
            payment.setAccountId(account.getId());
        }

        BigDecimal payAmount = updateReqVO.getPayAmount() != null
                ? updateReqVO.getPayAmount().setScale(2, RoundingMode.HALF_UP)
                : payment.getPayAmount();

        // 校验核销分配
        if (updateReqVO.getItems() != null) {
            validateReceiptItems(payment.getCurrency(), payment.getCustomerId(), payAmount, updateReqVO.getItems());
        }

        if (updateReqVO.getReceiptType() != null) {
            payment.setReceiptType(updateReqVO.getReceiptType());
        }
        if (updateReqVO.getPayAmount() != null) {
            payment.setPayAmount(payAmount);
        }
        if (updateReqVO.getReceiptDate() != null) {
            payment.setReceiptDate(updateReqVO.getReceiptDate());
        }
        payment.setBankRefNo(updateReqVO.getBankRefNo());
        payment.setHandlerName(updateReqVO.getHandlerName());
        payment.setRemark(updateReqVO.getRemark());

        paymentMapper.updateById(payment);

        if (updateReqVO.getItems() != null) {
            // 重建核销行
            paymentRefMapper.delete(new LambdaQueryWrapperX<PaymentRefDO>()
                    .eq(PaymentRefDO::getPaymentId, payment.getId()));
            saveReceiptRefs(payment.getId(), updateReqVO.getItems());
        }

        log.info("[updateReceipt] 修改收款单草稿成功: id={}", payment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceipt(Long id) {
        if (id == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(id);
        validateReceiptExistsAndTenant(payment);

        // 仅 PENDING 草稿可删
        if (!isDraft(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        paymentRefMapper.delete(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getPaymentId, id));
        paymentMapper.deleteById(id);
        log.info("[deleteReceipt] 软删除收款单草稿: id={}", id);
    }

    @Override
    public ReceiptRespVO getReceipt(Long id) {
        if (id == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(id);
        validateReceiptExistsAndTenant(payment);

        ReceiptRespVO vo = BeanUtils.toBean(payment, ReceiptRespVO.class);

        // 补充客户名与账户名
        if (payment.getCustomerId() != null) {
            CustomerDO customer = customerMapper.selectById(payment.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCompanyName());
            }
        }
        if (payment.getAccountId() != null) {
            AccountDO account = accountMapper.selectById(payment.getAccountId());
            if (account != null) {
                vo.setAccountName(account.getAccountName());
            }
        }

        // 补充核销明细
        List<PaymentRefDO> refs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getPaymentId, id)
                .eq(PaymentRefDO::getRefType, PaymentRefType.AR_ENTRY.getValue()));

        if (CollUtil.isNotEmpty(refs)) {
            List<Long> entryIds = CollectionUtils.convertList(refs, PaymentRefDO::getRefId);
            List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                    .inOrEmpty(LedgerEntryDO::getId, entryIds));
            Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(entries, LedgerEntryDO::getId);

            List<Long> shipmentIds = entries.stream().map(LedgerEntryDO::getRefId).filter(Objects::nonNull).distinct().toList();
            Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);

            List<ReceiptItemRespVO> itemVOs = new ArrayList<>();
            for (PaymentRefDO ref : refs) {
                ReceiptItemRespVO itemVO = new ReceiptItemRespVO();
                itemVO.setId(ref.getId());
                itemVO.setPaymentId(ref.getPaymentId());
                itemVO.setEntryId(ref.getRefId());
                itemVO.setEntryNo(ref.getEntryNo());
                itemVO.setBizDate(ref.getBizDate());
                itemVO.setAllocateAmount(ref.getAllocateAmount());
                itemVO.setBaseAmount(ref.getBaseAmount());
                itemVO.setLedgerEntryId(ref.getLedgerEntryId());

                LedgerEntryDO entry = entryMap.get(ref.getRefId());
                if (entry != null) {
                    itemVO.setSummary(entry.getRemark());
                    ShipmentDO shipment = shipmentMap.get(entry.getRefId());
                    if (shipment != null) {
                        itemVO.setContractNo(shipment.getContractNo());
                    }
                }
                itemVOs.add(itemVO);
            }
            vo.setItems(itemVOs);
            BigDecimal allocatedSum = itemVOs.stream()
                    .map(ReceiptItemRespVO::getAllocateAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setAllocatedTotalAmount(allocatedSum);
            BigDecimal pay = vo.getPayAmount() != null ? vo.getPayAmount() : BigDecimal.ZERO;
            BigDecimal unalloc = pay.subtract(allocatedSum);
            vo.setUnallocatedAmount(unalloc.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : unalloc);
        } else {
            vo.setItems(Collections.emptyList());
            vo.setAllocatedTotalAmount(BigDecimal.ZERO);
            vo.setUnallocatedAmount(vo.getPayAmount() != null ? vo.getPayAmount() : BigDecimal.ZERO);
        }

        return vo;
    }

    @Override
    public PageResult<ReceiptRespVO> getReceiptPage(ReceiptPageReqVO pageReqVO) {
        LambdaQueryWrapperX<PaymentDO> queryWrapper = new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getDirection, "IN")
                .eqIfPresent(PaymentDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(PaymentDO::getAccountId, pageReqVO.getAccountId())
                .eqIfPresent(PaymentDO::getReceiptType, pageReqVO.getReceiptType())
                .eqIfPresent(PaymentDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .eqIfPresent(PaymentDO::getCurrency, pageReqVO.getCurrency())
                .geIfPresent(PaymentDO::getReceiptDate, pageReqVO.getReceiptDateStart())
                .leIfPresent(PaymentDO::getReceiptDate, pageReqVO.getReceiptDateEnd())
                .likeIfPresent(PaymentDO::getPaymentNo, pageReqVO.getPaymentNo())
                .orderByDesc(PaymentDO::getId);

        PageResult<PaymentDO> pageResult = paymentMapper.selectPage(pageReqVO, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        List<Long> customerIds = pageResult.getList().stream().map(PaymentDO::getCustomerId).filter(Objects::nonNull).distinct().toList();
        List<Long> accountIds = pageResult.getList().stream().map(PaymentDO::getAccountId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> customerNameMap = batchLoadCustomerNames(customerIds);
        Map<Long, String> accountNameMap = batchLoadAccountNames(accountIds);

        // 防 N+1 批量快照模式：批量加载当前页收款单的核销分配合计
        List<Long> paymentIds = pageResult.getList().stream().map(PaymentDO::getId).filter(Objects::nonNull).toList();
        List<PaymentRefDO> allRefs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .inOrEmpty(PaymentRefDO::getPaymentId, paymentIds));
        Map<Long, BigDecimal> sumAllocatedMap = new HashMap<>();
        if (CollUtil.isNotEmpty(allRefs)) {
            for (PaymentRefDO ref : allRefs) {
                if (ref.getAllocateAmount() != null) {
                    sumAllocatedMap.merge(ref.getPaymentId(), ref.getAllocateAmount(), BigDecimal::add);
                }
            }
        }

        List<ReceiptRespVO> respList = new ArrayList<>();
        for (PaymentDO payment : pageResult.getList()) {
            ReceiptRespVO vo = BeanUtils.toBean(payment, ReceiptRespVO.class);
            vo.setCustomerName(customerNameMap.getOrDefault(payment.getCustomerId(), ""));
            vo.setAccountName(accountNameMap.getOrDefault(payment.getAccountId(), ""));
            BigDecimal allocated = sumAllocatedMap.getOrDefault(payment.getId(), BigDecimal.ZERO);
            vo.setAllocatedTotalAmount(allocated);
            BigDecimal pay = payment.getPayAmount() != null ? payment.getPayAmount() : BigDecimal.ZERO;
            BigDecimal unalloc = pay.subtract(allocated);
            vo.setUnallocatedAmount(unalloc.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : unalloc);
            respList.add(vo);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReceipt(Long id) {
        if (id == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectById(id);
        validateReceiptExistsAndTenant(payment);

        // 状态守卫：仅 PENDING 草稿可提交（对齐 Spec 最小契约原则）
        if (!isDraft(payment.getApprovalStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 校验核销明细
        List<PaymentRefDO> refs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getPaymentId, id)
                .eq(PaymentRefDO::getRefType, PaymentRefType.AR_ENTRY.getValue()));

        if (CollUtil.isNotEmpty(refs)) {
            List<ReceiptItemReqVO> itemReqs = CollectionUtils.convertList(refs, r -> {
                ReceiptItemReqVO item = new ReceiptItemReqVO();
                item.setEntryId(r.getRefId());
                item.setAllocateAmount(r.getAllocateAmount());
                return item;
            });
            validateReceiptItems(payment.getCurrency(), payment.getCustomerId(), payment.getPayAmount(), itemReqs);
        }

        // 调用 02 号审批引擎发起审批
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType(ApprovalHandler.BIZ_TYPE_CUSTOMER_RECEIPT);
        submitReqVO.setBizId(id);
        submitReqVO.setComment("提交客户收款单审批");

        approvalInstanceService.submitInstance(submitReqVO);
        log.info("[submitReceipt] 收款单提交审批成功: id={}", id);
    }

    @Override
    public List<AllocatePreviewRespVO> allocatePreview(AllocatePreviewReqVO reqVO) {
        if (reqVO == null || reqVO.getCustomerId() == null || StrUtil.isBlank(reqVO.getCurrency())) {
            return Collections.emptyList();
        }

        // 1. 查询该客户该币种下所有 AR_INC 分录，按业务日期升序（FIFO）
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .eq(LedgerEntryDO::getPartyId, reqVO.getCustomerId())
                .eq(LedgerEntryDO::getCurrency, reqVO.getCurrency())
                .eq(LedgerEntryDO::getCategory, "AR_INC")
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }

        // 2. 批量聚合未核销余额
        List<Long> entryIds = CollectionUtils.convertList(entries, LedgerEntryDO::getId);
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(entries, LedgerEntryDO::getId);
        Map<Long, BigDecimal> unallocMap = batchLoadUnallocatedBalanceMap(entryIds, entryMap);

        // 3. 批量查询发货单快照信息
        List<Long> shipmentIds = entries.stream().map(LedgerEntryDO::getRefId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);

        // 4. FIFO 顺序分配试算（严格按 bizDate 升序，同日按 id 升序）
        List<LedgerEntryDO> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparing(LedgerEntryDO::getBizDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LedgerEntryDO::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        BigDecimal remainingPay = reqVO.getPayAmount() != null ? reqVO.getPayAmount() : BigDecimal.ZERO;
        List<AllocatePreviewRespVO> result = new ArrayList<>();

        for (LedgerEntryDO entry : sortedEntries) {
            BigDecimal unalloc = unallocMap.getOrDefault(entry.getId(), BigDecimal.ZERO);
            if (unalloc.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocate = BigDecimal.ZERO;
            if (remainingPay.compareTo(BigDecimal.ZERO) > 0) {
                allocate = remainingPay.min(unalloc);
                remainingPay = remainingPay.subtract(allocate);
            }

            ShipmentDO shipment = shipmentMap.get(entry.getRefId());
            AllocatePreviewRespVO vo = new AllocatePreviewRespVO();
            vo.setEntryId(entry.getId());
            vo.setBizDate(entry.getBizDate());
            vo.setRefType(entry.getRefType());
            vo.setRefId(entry.getRefId());
            vo.setRefItemId(entry.getRefItemId());
            vo.setRefNo(shipment != null ? shipment.getShipmentNo() : "");
            vo.setContractNo(shipment != null ? shipment.getContractNo() : "");
            vo.setProductName(entry.getRemark());
            vo.setCurrency(entry.getCurrency());
            vo.setOriginalAmount(entry.getOriginalAmount());
            vo.setUnallocatedAmount(unalloc);
            vo.setAllocateAmount(allocate);

            result.add(vo);
        }

        return result;
    }

    @Override
    public List<ArBalanceRespVO> getArBalanceList(Long customerId, String currency) {
        if (customerId == null || StrUtil.isBlank(currency)) {
            return Collections.emptyList();
        }

        // 1. 查询该客户该币种的 AR_INC 分录
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .eq(LedgerEntryDO::getPartyId, customerId)
                .eq(LedgerEntryDO::getCurrency, currency)
                .eq(LedgerEntryDO::getCategory, "AR_INC")
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }

        // 2. 批量聚合未核销余额
        List<Long> entryIds = CollectionUtils.convertList(entries, LedgerEntryDO::getId);
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(entries, LedgerEntryDO::getId);
        Map<Long, BigDecimal> unallocMap = batchLoadUnallocatedBalanceMap(entryIds, entryMap);

        List<Long> shipmentIds = entries.stream().map(LedgerEntryDO::getRefId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);

        List<ArBalanceRespVO> result = new ArrayList<>();
        for (LedgerEntryDO entry : entries) {
            BigDecimal unalloc = unallocMap.getOrDefault(entry.getId(), BigDecimal.ZERO);
            if (unalloc.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocated = entry.getOriginalAmount().subtract(unalloc);
            if (allocated.compareTo(BigDecimal.ZERO) < 0) {
                allocated = BigDecimal.ZERO;
            }

            ShipmentDO shipment = shipmentMap.get(entry.getRefId());
            ArBalanceRespVO vo = new ArBalanceRespVO();
            vo.setEntryId(entry.getId());
            vo.setBizDate(entry.getBizDate());
            vo.setRefType(entry.getRefType());
            vo.setRefId(entry.getRefId());
            vo.setRefItemId(entry.getRefItemId());
            vo.setRefNo(shipment != null ? shipment.getShipmentNo() : "");
            vo.setContractId(shipment != null ? shipment.getSalesContractId() : null);
            vo.setContractNo(shipment != null ? shipment.getContractNo() : "");
            vo.setProductName(entry.getRemark());
            vo.setCurrency(entry.getCurrency());
            vo.setOriginalAmount(entry.getOriginalAmount());
            vo.setAllocatedAmount(allocated);
            vo.setUnallocatedAmount(unalloc);

            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseReceipt(Long id) {
        if (id == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        PaymentDO payment = paymentMapper.selectOne(new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getId, id)
                .last("FOR UPDATE"));
        validateReceiptExistsAndTenant(payment);

        // 状态守卫：仅已过账 COMPLETED 可红冲；已是 REVERSED 则抛 RECEIPT_REVERSAL_NOT_ALLOW（红冲幂等）
        if (PaymentStatus.REVERSED.getValue().equalsIgnoreCase(payment.getStatus())) {
            throw exception(RECEIPT_REVERSAL_NOT_ALLOW);
        }
        if (!PaymentStatus.COMPLETED.getValue().equalsIgnoreCase(payment.getStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 若是 ADVANCE 预收款单，检查是否已被后续预收转核销引用；若已有核销引用则不允许红冲
        if (ReceiptType.ADVANCE.getValue().equalsIgnoreCase(payment.getReceiptType())) {
            Long advanceWriteoffCount = ledgerEntryMapper.selectCount(new LambdaQueryWrapperX<LedgerEntryDO>()
                    .eq(LedgerEntryDO::getRefType, "RECEIPT")
                    .eq(LedgerEntryDO::getRefId, payment.getId())
                    .eq(LedgerEntryDO::getCategory, LedgerCategory.ADVANCE_WRITE_OFF.getValue()));
            if (advanceWriteoffCount != null && advanceWriteoffCount > 0) {
                log.warn("[reverseReceipt] 预收款单已被后续预收转核销引用，禁止红冲: paymentId={}", id);
                throw exception(RECEIPT_REVERSAL_NOT_ALLOW);
            }
        }

        // 1. 查询原入账分录（RECEIPT 或 ADVANCE）
        LedgerEntryDO origEntry = ledgerEntryMapper.selectOne(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, "RECEIPT")
                .eq(LedgerEntryDO::getRefId, payment.getId())
                .in(LedgerEntryDO::getCategory, List.of(LedgerCategory.RECEIPT.getValue(), LedgerCategory.ADVANCE.getValue())));

        // 2. 写 1：追加反向 RECEIPT_REVERSAL 分录（reversal_of_id 回指原分录）
        BigDecimal payAmount = payment.getPayAmount() != null ? payment.getPayAmount() : BigDecimal.ZERO;
        BigDecimal rate = origEntry != null && origEntry.getExchangeRate() != null
                ? origEntry.getExchangeRate()
                : (payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE);
        BigDecimal baseAmount = origEntry != null && origEntry.getBaseAmount() != null
                ? origEntry.getBaseAmount()
                : payAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        LedgerEntryDO reversal = new LedgerEntryDO();
        reversal.setBizDate(LocalDate.now());
        reversal.setPartyType("CUSTOMER");
        reversal.setPartyId(payment.getCustomerId());
        reversal.setAccountId(payment.getAccountId());
        reversal.setCategory(LedgerCategory.RECEIPT_REVERSAL.getValue());
        reversal.setDirection("DEBIT");
        reversal.setCurrency(payment.getCurrency());
        reversal.setExchangeRate(rate);
        reversal.setOriginalAmount(payAmount);
        reversal.setBaseAmount(baseAmount);
        reversal.setRefType("RECEIPT");
        reversal.setRefId(payment.getId());
        reversal.setReversalOfId(origEntry != null ? origEntry.getId() : null);
        reversal.setRemark("红冲收款：" + (payment.getPaymentNo() != null ? payment.getPaymentNo() : ""));
        ledgerService.append(reversal);

        // 3. 写 2：资金账户 current_balance -= amt 条件更新
        accountService.updateBalance(payment.getAccountId(), payAmount.negate());

        // 4. 写 3：主表状态置为 REVERSED（此操作天然使得 payment_ref 不再满足 status='COMPLETED'，核销行自动重开，未核销余额恢复）
        payment.setStatus(PaymentStatus.REVERSED.getValue());
        paymentMapper.updateById(payment);

        log.info("[reverseReceipt] 收款单红冲成功: id={}, paymentNo={}", payment.getId(), payment.getPaymentNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceWriteoff(AdvanceWriteoffReqVO reqVO) {
        if (reqVO == null || reqVO.getPaymentId() == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        PaymentDO payment = paymentMapper.selectOne(new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getId, reqVO.getPaymentId())
                .last("FOR UPDATE"));
        validateReceiptExistsAndTenant(payment);

        // 状态与类型守卫：必须为已过账 COMPLETED 的 ADVANCE 单
        if (!PaymentStatus.COMPLETED.getValue().equalsIgnoreCase(payment.getStatus())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }
        if (!ReceiptType.ADVANCE.getValue().equalsIgnoreCase(payment.getReceiptType())) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }

        // 1. 预收余额计算（ADVANCE 入账 − ADVANCE_WRITE_OFF）
        BigDecimal advanceWrittenOff = calculateAdvanceWrittenOff(payment.getId());
        BigDecimal totalAdvance = payment.getPayAmount() != null ? payment.getPayAmount() : BigDecimal.ZERO;
        BigDecimal advanceBalance = totalAdvance.subtract(advanceWrittenOff);
        if (advanceBalance.compareTo(BigDecimal.ZERO) < 0) {
            advanceBalance = BigDecimal.ZERO;
        }

        BigDecimal totalAllocating = BigDecimal.ZERO;
        for (ReceiptItemReqVO item : reqVO.getItems()) {
            if (item.getAllocateAmount() != null && item.getAllocateAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalAllocating = totalAllocating.add(item.getAllocateAmount());
            }
        }

        if (totalAllocating.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 守卫：≤预收余额
        if (totalAllocating.compareTo(advanceBalance) > 0) {
            log.warn("[advanceWriteoff] 预收核销金额超出预收余额: paymentId={}, totalAllocating={}, advanceBalance={}",
                    payment.getId(), totalAllocating, advanceBalance);
            throw exception(RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH);
        }

        // 2. 校验待核销 AR 分录（同币种、同客户、≤未核销余额并加悲观锁）
        List<Long> entryIds = reqVO.getItems().stream()
                .map(ReceiptItemReqVO::getEntryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (CollUtil.isEmpty(entryIds)) {
            return;
        }

        // 带行级悲观锁批量查询，防止并发幻读超核
        List<LedgerEntryDO> origEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getId, entryIds)
                .last("FOR UPDATE"));
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(origEntries, LedgerEntryDO::getId);
        Map<Long, BigDecimal> unallocMap = batchLoadUnallocatedBalanceMap(entryIds, entryMap);

        // 聚合单次请求中同 entryId 的分配额，防止单次请求多次引用同一 entry 突破未核销余额
        Map<Long, BigDecimal> requestedByEntry = new HashMap<>();
        for (ReceiptItemReqVO item : reqVO.getItems()) {
            if (item.getAllocateAmount() != null && item.getAllocateAmount().compareTo(BigDecimal.ZERO) > 0) {
                requestedByEntry.merge(item.getEntryId(), item.getAllocateAmount(), BigDecimal::add);
            }
        }

        for (Map.Entry<Long, BigDecimal> entryReq : requestedByEntry.entrySet()) {
            LedgerEntryDO origEntry = entryMap.get(entryReq.getKey());
            if (origEntry == null) {
                throw exception(RECEIPT_POST_CONFLICT);
            }
            if (!payment.getCustomerId().equals(origEntry.getPartyId())) {
                throw exception(RECEIPT_CUSTOMER_MISMATCH);
            }
            if (!payment.getCurrency().equalsIgnoreCase(origEntry.getCurrency())) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }
            BigDecimal currentRemain = unallocMap.getOrDefault(entryReq.getKey(), BigDecimal.ZERO);
            if (entryReq.getValue().compareTo(currentRemain) > 0) {
                log.warn("[advanceWriteoff] 预收核销金额超出分录未核销余额冲突: entryId={}, requested={}, currentRemain={}",
                        entryReq.getKey(), entryReq.getValue(), currentRemain);
                throw exception(RECEIPT_POST_CONFLICT);
            }
        }

        List<Long> shipmentIds = origEntries.stream().map(LedgerEntryDO::getRefId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);
        BigDecimal rate = payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE;
        LocalDate now = LocalDate.now();

        // 3. 在原单追加核销行（ref_type=AR_ENTRY），写 ADVANCE_WRITE_OFF 分录
        for (ReceiptItemReqVO item : reqVO.getItems()) {
            if (item.getAllocateAmount() == null || item.getAllocateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LedgerEntryDO origEntry = entryMap.get(item.getEntryId());
            ShipmentDO shipment = origEntry != null ? shipmentMap.get(origEntry.getRefId()) : null;
            BigDecimal refBaseAmount = item.getAllocateAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

            // 写 ADVANCE_WRITE_OFF 分录
            LedgerEntryDO writeOffEntry = new LedgerEntryDO();
            writeOffEntry.setBizDate(now);
            writeOffEntry.setPartyType("CUSTOMER");
            writeOffEntry.setPartyId(payment.getCustomerId());
            writeOffEntry.setAccountId(null); // 往来核销不影响资金账户流水
            writeOffEntry.setCategory(LedgerCategory.ADVANCE_WRITE_OFF.getValue());
            writeOffEntry.setDirection("CREDIT");
            writeOffEntry.setCurrency(payment.getCurrency());
            writeOffEntry.setExchangeRate(rate);
            writeOffEntry.setOriginalAmount(item.getAllocateAmount());
            writeOffEntry.setBaseAmount(refBaseAmount);
            writeOffEntry.setRefType("RECEIPT");
            writeOffEntry.setRefId(payment.getId());
            writeOffEntry.setRefItemId(item.getEntryId());
            writeOffEntry.setRemark("预收核销：" + (shipment != null ? shipment.getShipmentNo() : (origEntry != null ? origEntry.getRemark() : "")));
            Long writeOffEntryId = ledgerService.append(writeOffEntry);

            // 追加 payment_ref 核销行
            PaymentRefDO ref = new PaymentRefDO();
            ref.setPaymentId(payment.getId());
            ref.setRefType(PaymentRefType.AR_ENTRY.getValue());
            ref.setRefId(item.getEntryId());
            ref.setAllocateAmount(item.getAllocateAmount());
            ref.setEntryNo(shipment != null ? shipment.getShipmentNo() : (origEntry != null ? String.valueOf(origEntry.getRefId()) : ""));
            ref.setBizDate(now);
            ref.setBaseAmount(refBaseAmount);
            ref.setLedgerEntryId(writeOffEntryId);
            paymentRefMapper.insert(ref);
        }

        // 4. 更新原单 prepayUsedAmount 快照
        payment.setPrepayUsedAmount(advanceWrittenOff.add(totalAllocating));
        paymentMapper.updateById(payment);

        log.info("[advanceWriteoff] 预收转核销成功: paymentId={}, totalAllocated={}", payment.getId(), totalAllocating);
    }

    private BigDecimal calculateAdvanceWrittenOff(Long paymentId) {
        if (paymentId == null) {
            return BigDecimal.ZERO;
        }
        List<LedgerEntryDO> list = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, "RECEIPT")
                .eq(LedgerEntryDO::getRefId, paymentId)
                .eq(LedgerEntryDO::getCategory, LedgerCategory.ADVANCE_WRITE_OFF.getValue()));
        if (CollUtil.isEmpty(list)) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(LedgerEntryDO::getOriginalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== 私有辅助方法 ====================

    private void validateReceiptExistsAndTenant(PaymentDO payment) {
        if (payment == null || !"IN".equalsIgnoreCase(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        validateTenant(payment.getTenantId());
    }

    private void validateReceiptItems(String currency, Long customerId, BigDecimal payAmount, List<ReceiptItemReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }

        List<Long> entryIds = CollectionUtils.convertList(items, ReceiptItemReqVO::getEntryId);
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getId, entryIds));
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(entries, LedgerEntryDO::getId);
        Map<Long, BigDecimal> unallocMap = batchLoadUnallocatedBalanceMap(entryIds, entryMap);

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (ReceiptItemReqVO item : items) {
            LedgerEntryDO entry = entryMap.get(item.getEntryId());
            if (entry == null || !entry.getPartyId().equals(customerId)) {
                throw exception(RECEIPT_CUSTOMER_MISMATCH);
            }
            if (!entry.getCurrency().equalsIgnoreCase(currency)) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }
            BigDecimal maxAllowed = unallocMap.getOrDefault(item.getEntryId(), BigDecimal.ZERO);
            if (item.getAllocateAmount() != null && item.getAllocateAmount().compareTo(maxAllowed) > 0) {
                throw exception(RECEIPT_ALLOCATE_EXCEED);
            }
            if (item.getAllocateAmount() != null) {
                totalAllocated = totalAllocated.add(item.getAllocateAmount());
            }
        }

        if (payAmount != null && totalAllocated.compareTo(payAmount) > 0) {
            throw exception(RECEIPT_ALLOCATE_EXCEED);
        }
    }

    private void saveReceiptRefs(Long paymentId, List<ReceiptItemReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<Long> entryIds = CollectionUtils.convertList(items, ReceiptItemReqVO::getEntryId);
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getId, entryIds));
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(entries, LedgerEntryDO::getId);

        List<Long> shipmentIds = entries.stream().map(LedgerEntryDO::getRefId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ShipmentDO> shipmentMap = batchLoadShipmentMap(shipmentIds);

        for (ReceiptItemReqVO item : items) {
            LedgerEntryDO entry = entryMap.get(item.getEntryId());
            ShipmentDO shipment = entry != null ? shipmentMap.get(entry.getRefId()) : null;

            PaymentRefDO ref = new PaymentRefDO();
            ref.setPaymentId(paymentId);
            ref.setRefType(PaymentRefType.AR_ENTRY.getValue());
            ref.setRefId(item.getEntryId());
            ref.setAllocateAmount(item.getAllocateAmount());
            ref.setEntryNo(shipment != null ? shipment.getShipmentNo() : (entry != null ? String.valueOf(entry.getRefId()) : ""));
            ref.setBizDate(entry != null ? entry.getBizDate() : LocalDate.now());
            ref.setBaseAmount(BigDecimal.ZERO);

            paymentRefMapper.insert(ref);
        }
    }

    private String generateReceiptNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = RECEIPT_NO_PREFIX + DATE_FORMATTER.format(targetDate) + "-";
        List<PaymentDO> candidates = paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(prefix);

        long maxNum = 0;
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
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            long nextNum = maxNum + 1 + i;
            String paymentNo = String.format("%s%0" + maxDigits + "d", prefix, nextNum);
            PaymentDO existing = paymentMapper.selectByPaymentNoIncludingDeleted(paymentNo);
            if (existing == null) {
                return paymentNo;
            }
        }
        log.error("[generateReceiptNo] 收款单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(PAYMENT_NO_EXHAUSTED);
    }

    private Map<Long, BigDecimal> batchLoadAllocatedAmountMap(Collection<Long> entryIds) {
        if (CollUtil.isEmpty(entryIds)) {
            return Collections.emptyMap();
        }
        return EntryAllocatedSumDTO.toMap(paymentRefMapper.selectSumAllocatedAmountByEntryIds(entryIds));
    }

    private Map<Long, BigDecimal> batchLoadUnallocatedBalanceMap(Collection<Long> entryIds, Map<Long, LedgerEntryDO> entryMap) {
        if (CollUtil.isEmpty(entryIds)) {
            return Collections.emptyMap();
        }

        // 1. 发货红冲冲减 (AR_INC_REVERSAL)
        List<LedgerEntryDO> reversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getReversalOfId, entryIds));
        Map<Long, BigDecimal> reversalMap = new HashMap<>();
        if (CollUtil.isNotEmpty(reversals)) {
            for (LedgerEntryDO rev : reversals) {
                if (rev.getReversalOfId() != null && rev.getOriginalAmount() != null) {
                    reversalMap.merge(rev.getReversalOfId(), rev.getOriginalAmount(), BigDecimal::add);
                }
            }
        }

        // 2. 收款单已核销额 (COMPLETED payment_ref)
        Map<Long, BigDecimal> allocatedMap = batchLoadAllocatedAmountMap(entryIds);

        // 3. 退货应收冲减 (AR_RETURN_REVERSAL)
        List<LedgerEntryDO> returnReversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getCategory, "AR_RETURN_REVERSAL")
                .inOrEmpty(LedgerEntryDO::getRefItemId, entryIds));
        Map<Long, BigDecimal> returnReversalMap = new HashMap<>();
        if (CollUtil.isNotEmpty(returnReversals)) {
            for (LedgerEntryDO ret : returnReversals) {
                if (ret.getRefItemId() != null && ret.getOriginalAmount() != null) {
                    returnReversalMap.merge(ret.getRefItemId(), ret.getOriginalAmount(), BigDecimal::add);
                }
            }
        }

        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Long entryId : entryIds) {
            LedgerEntryDO entry = entryMap.get(entryId);
            if (entry == null) {
                continue;
            }
            BigDecimal orig = entry.getOriginalAmount() != null ? entry.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal rev = reversalMap.getOrDefault(entryId, BigDecimal.ZERO);
            BigDecimal alloc = allocatedMap.getOrDefault(entryId, BigDecimal.ZERO);
            BigDecimal ret = returnReversalMap.getOrDefault(entryId, BigDecimal.ZERO);

            BigDecimal unalloc = orig.subtract(rev).subtract(alloc).subtract(ret);
            if (unalloc.compareTo(BigDecimal.ZERO) < 0) {
                unalloc = BigDecimal.ZERO;
            }
            resultMap.put(entryId, unalloc);
        }
        return resultMap;
    }

    private Map<Long, String> batchLoadCustomerNames(Collection<Long> customerIds) {
        if (CollUtil.isEmpty(customerIds)) {
            return Collections.emptyMap();
        }
        List<CustomerDO> list = customerMapper.selectList(new LambdaQueryWrapperX<CustomerDO>()
                .inOrEmpty(CustomerDO::getId, customerIds));
        Map<Long, String> map = new HashMap<>();
        if (CollUtil.isNotEmpty(list)) {
            for (CustomerDO c : list) {
                map.put(c.getId(), c.getCompanyName());
            }
        }
        return map;
    }

    private boolean isDraft(String status) {
        return ApplyStatus.isDraft(status);
    }

    private Map<Long, String> batchLoadAccountNames(Collection<Long> accountIds) {
        if (CollUtil.isEmpty(accountIds)) {
            return Collections.emptyMap();
        }
        List<AccountDO> list = accountMapper.selectList(new LambdaQueryWrapperX<AccountDO>()
                .inOrEmpty(AccountDO::getId, accountIds));
        Map<Long, String> map = new HashMap<>();
        if (CollUtil.isNotEmpty(list)) {
            for (AccountDO a : list) {
                map.put(a.getId(), a.getAccountName());
            }
        }
        return map;
    }

    private Map<Long, ShipmentDO> batchLoadShipmentMap(Collection<Long> shipmentIds) {
        if (CollUtil.isEmpty(shipmentIds)) {
            return Collections.emptyMap();
        }
        List<ShipmentDO> list = shipmentMapper.selectList(new LambdaQueryWrapperX<ShipmentDO>()
                .inOrEmpty(ShipmentDO::getId, shipmentIds));
        return CollectionUtils.convertMap(list, ShipmentDO::getId);
    }

}

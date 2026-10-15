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
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.Direction;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentRefType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinanceAccountService;
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
    private PaymentApplyService paymentApplyService;

    @Resource
    private FinanceAccountService financeAccountService;

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

}

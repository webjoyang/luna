package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.seam.FinanceAccountSeam;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.seam.FinanceLedgerSeam;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
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
 * 财务管理 - 付款登记服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PaymentServiceImpl implements PaymentService {

    /**
     * 收付款单号前缀（ADR-0006：PAY + yyyyMMdd + 3位流水）。
     * 【共享件清单冲突挂账】：本前缀为 06 号（OUT 侧）与 10 号（IN 侧）共享，
     * 合并时若 10 号需按方向分流前缀在此统一收口。
     */
    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("(\\d+)$");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 5;

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
    private FinanceAccountSeam financeAccountSeam;

    @Resource
    private FinanceLedgerSeam financeLedgerSeam;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT", subType = "REGISTER", bizNo = "{{#_ret}}", success = "付款登记成功【{{#_ret}}】")
    public Long registerPayment(PaymentRegisterReqVO reqVO) {
        // 1. 守卫供应商：供应商必须存在
        if (reqVO.getSupplierId() == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }
        SupplierDO supplier = supplierMapper.selectById(reqVO.getSupplierId());
        if (supplier == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }

        // 2. 守卫核销明细 items
        List<PaymentRegisterItemReqVO> items = reqVO.getItems();
        if (CollUtil.isEmpty(items)) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        Set<Long> applyIds = new HashSet<>();
        for (PaymentRegisterItemReqVO item : items) {
            if (item.getApplyId() == null || !applyIds.add(item.getApplyId())) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }
        }

        List<PaymentApplyDO> applies = paymentApplyMapper.selectBatchIds(applyIds);
        Map<Long, PaymentApplyDO> applyMap = CollUtil.isNotEmpty(applies)
                ? applies.stream().collect(Collectors.toMap(PaymentApplyDO::getId, Function.identity()))
                : Collections.emptyMap();

        // 多申请按申请创建时间升序排序（按申请创建时间顺序分配）
        List<PaymentRegisterItemReqVO> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            PaymentApplyDO applyA = applyMap.get(a.getApplyId());
            PaymentApplyDO applyB = applyMap.get(b.getApplyId());
            if (applyA != null && applyB != null && applyA.getCreateTime() != null && applyB.getCreateTime() != null) {
                int cmp = applyA.getCreateTime().compareTo(applyB.getCreateTime());
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Long.compare(a.getApplyId(), b.getApplyId());
        });

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (PaymentRegisterItemReqVO item : sortedItems) {
            PaymentApplyDO apply = applyMap.get(item.getApplyId());
            if (apply == null) {
                throw exception(PAYMENT_APPLY_NOT_EXISTS);
            }
            // 校验是否属于同一供应商
            if (!Objects.equals(apply.getSupplierId(), reqVO.getSupplierId())) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }
            // 校验审批状态是否为 APPROVED
            if (!"APPROVED".equalsIgnoreCase(apply.getApprovalStatus())) {
                throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
            }
            if (item.getAllocateAmount() == null || item.getAllocateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PAYMENT_AMOUNT_INVALID);
            }

            // 计算该申请已核销金额 sumAllocated
            List<PaymentRefDO> existingRefs = paymentRefMapper.selectListByRefTypeAndRefId(
                    PaymentRefTypeEnum.PAY_APPLY.getValue(), apply.getId());
            BigDecimal sumAllocated = BigDecimal.ZERO;
            if (CollUtil.isNotEmpty(existingRefs)) {
                for (PaymentRefDO ref : existingRefs) {
                    if (ref.getAllocateAmount() != null) {
                        sumAllocated = sumAllocated.add(ref.getAllocateAmount());
                    }
                }
            }

            // 剩余可核销金额 remaining
            BigDecimal remaining = apply.getAmount().subtract(sumAllocated);
            if (item.getAllocateAmount().compareTo(remaining) > 0) {
                throw exception(PAYMENT_ALLOCATE_EXCEED);
            }

            totalAllocated = totalAllocated.add(item.getAllocateAmount());
        }

        BigDecimal payAmount = reqVO.getPayAmount() != null ? reqVO.getPayAmount() : BigDecimal.ZERO;
        BigDecimal prepayUsed = reqVO.getPrepayUsedAmount() != null ? reqVO.getPrepayUsedAmount() : BigDecimal.ZERO;
        if (payAmount.compareTo(BigDecimal.ZERO) < 0 || prepayUsed.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        // 校验钱是否够分：totalAllocated <= payAmount + prepayUsedAmount
        BigDecimal totalFunds = payAmount.add(prepayUsed);
        if (totalAllocated.compareTo(totalFunds) > 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        // 3. 预付守卫：若 prepayUsedAmount > 0，校验供应商预付余额
        if (prepayUsed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal prepayBalance = paymentApplyService.getPrepayBalance(reqVO.getSupplierId());
            if (prepayBalance == null || prepayUsed.compareTo(prepayBalance) > 0) {
                throw exception(PAYMENT_PREPAY_BALANCE_NOT_ENOUGH);
            }
        }

        // 4. 账户守卫：若 payAmount > 0，扣减资金账户余额
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (reqVO.getAccountId() == null) {
                throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
            }
            financeAccountSeam.deductAccountBalance(reqVO.getAccountId(), payAmount);
        }

        // 5. 生成付款单号：PAY + yyyyMMdd + 3位日流水（ADR-0006）
        String paymentNo = generatePaymentNoWithRetry();

        // 6. 插入收付款单主表 PaymentDO
        PaymentDO payment = PaymentDO.builder()
                .paymentNo(paymentNo)
                .direction(PaymentDirectionEnum.OUT.getValue())
                .supplierId(reqVO.getSupplierId())
                .accountId(reqVO.getAccountId())
                .payAmount(payAmount)
                .prepayUsedAmount(prepayUsed)
                .status(PaymentStatusEnum.COMPLETED.getValue())
                .remark(reqVO.getRemark())
                .build();
        paymentMapper.insert(payment);

        // 7. 插入收付款核销子表 PaymentRefDO（按创建时间顺序分配）
        for (PaymentRegisterItemReqVO item : sortedItems) {
            PaymentApplyDO apply = applyMap.get(item.getApplyId());
            PaymentRefDO ref = PaymentRefDO.builder()
                    .paymentId(payment.getId())
                    .refType(PaymentRefTypeEnum.PAY_APPLY.getValue())
                    .refId(item.getApplyId())
                    .applyNo(apply.getApplyNo()) // 快照申请单号
                    .allocateAmount(item.getAllocateAmount())
                    .build();
            paymentRefMapper.insert(ref);
        }

        // 8. 记录资金账户流出流水
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeAccountSeam.recordAccountOutflow(reqVO.getAccountId(), payAmount, payment.getId(), paymentNo);
        }

        // 9. 追加经营分录账（OUT_SUPPLIER）
        financeLedgerSeam.appendSupplierPaymentEntry(payment.getId(), paymentNo, reqVO.getSupplierId(),
                totalAllocated, payAmount, prepayUsed);

        // 10. 返回付款单ID
        return payment.getId();
    }

    @Override
    public PageResult<PaymentRespVO> getPaymentPage(PaymentPageReqVO reqVO) {
        PageResult<PaymentDO> pageResult = paymentMapper.selectPageByDirection(reqVO, PaymentDirectionEnum.OUT.getValue());
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }

        // 收集 supplierId 批量查询供应商名称
        Set<Long> supplierIds = pageResult.getList().stream()
                .map(PaymentDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SupplierDO> supplierMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(supplierIds)) {
            List<SupplierDO> suppliers = supplierMapper.selectBatchIds(supplierIds);
            if (CollUtil.isNotEmpty(suppliers)) {
                supplierMap = suppliers.stream().collect(Collectors.toMap(SupplierDO::getId, Function.identity()));
            }
        }

        // 收集 paymentId 批量查询核销行
        List<Long> paymentIds = pageResult.getList().stream()
                .map(PaymentDO::getId)
                .collect(Collectors.toList());
        List<PaymentRefDO> allRefs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .in(PaymentRefDO::getPaymentId, paymentIds));
        Map<Long, List<PaymentRefDO>> refMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(allRefs)) {
            refMap = allRefs.stream().collect(Collectors.groupingBy(PaymentRefDO::getPaymentId));
        }

        Map<Long, SupplierDO> finalSupplierMap = supplierMap;
        Map<Long, List<PaymentRefDO>> finalRefMap = refMap;
        List<PaymentRespVO> respList = pageResult.getList().stream().map(payment -> {
            SupplierDO supplier = finalSupplierMap.get(payment.getSupplierId());
            String supplierName = supplier != null ? supplier.getCompanyName() : null;
            List<PaymentRefDO> refs = finalRefMap.getOrDefault(payment.getId(), Collections.emptyList());
            List<PaymentRefRespVO> itemVOs = refs.stream().map(ref -> PaymentRefRespVO.builder()
                    .id(ref.getId())
                    .paymentId(ref.getPaymentId())
                    .refType(ref.getRefType())
                    .refId(ref.getRefId())
                    .applyNo(ref.getApplyNo())
                    .allocateAmount(ref.getAllocateAmount())
                    .build()).collect(Collectors.toList());
            return convertToRespVO(payment, supplierName, itemVOs);
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public PaymentRespVO getPayment(Long id) {
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw exception(PAYMENT_NOT_EXISTS);
        }
        SupplierDO supplier = null;
        if (payment.getSupplierId() != null) {
            supplier = supplierMapper.selectById(payment.getSupplierId());
        }
        List<PaymentRefDO> refs = paymentRefMapper.selectListByPaymentId(id);
        List<PaymentRefRespVO> itemRespVOs = new ArrayList<>();
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                itemRespVOs.add(PaymentRefRespVO.builder()
                        .id(ref.getId())
                        .paymentId(ref.getPaymentId())
                        .refType(ref.getRefType())
                        .refId(ref.getRefId())
                        .applyNo(ref.getApplyNo())
                        .allocateAmount(ref.getAllocateAmount())
                        .build());
            }
        }
        return convertToRespVO(payment, supplier != null ? supplier.getCompanyName() : null, itemRespVOs);
    }

    private PaymentRespVO convertToRespVO(PaymentDO payment, String supplierName, List<PaymentRefRespVO> items) {
        return PaymentRespVO.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .direction(payment.getDirection())
                .supplierId(payment.getSupplierId())
                .supplierName(supplierName)
                .accountId(payment.getAccountId())
                .payAmount(payment.getPayAmount())
                .prepayUsedAmount(payment.getPrepayUsedAmount())
                .status(payment.getStatus())
                .remark(payment.getRemark())
                .createTime(payment.getCreateTime())
                .items(items)
                .build();
    }

    /**
     * 生成付款单号：PAY + 8位年月日 + 3位日流水（超 999 自然扩位）
     */
    private String generatePaymentNo() {
        LocalDate today = LocalDate.now();
        String prefix = PAYMENT_NO_PREFIX + today.format(DATE_FORMATTER);
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
                long num;
                try {
                    num = Long.parseLong(digits);
                } catch (NumberFormatException ignore) {
                    continue;
                }
                if (num > maxNum) {
                    maxNum = num;
                    maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                }
            }
        }
        if (maxNum == 0L) {
            return prefix + String.format("%0" + NO_MIN_DIGITS + "d", 1);
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    /**
     * 自动取号（撞号有限次顺延重试）
     */
    private String generatePaymentNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generatePaymentNo();
            if (paymentMapper.selectByPaymentNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(PAYMENT_AMOUNT_INVALID);
    }

}

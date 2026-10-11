package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplySaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.*;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 付款申请服务实现
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PaymentApplyServiceImpl implements PaymentApplyService {

    private static final String APPLY_NO_PREFIX = "FK";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final int MAX_GENERATE_RETRY = 5;

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Override
    public PageResult<PaymentApplyRespVO> getPaymentApplyPage(PaymentApplyPageReqVO pageReqVO) {
        PageResult<PaymentApplyDO> pageResult = paymentApplyMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        List<Long> applyIds = pageResult.getList().stream()
                .map(PaymentApplyDO::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, BigDecimal> allocatedMap = new HashMap<>();
        if (CollUtil.isNotEmpty(applyIds)) {
            List<PaymentRefDO> refs = paymentRefMapper.selectListByRefTypeAndRefIds("PAY_APPLY", applyIds);
            if (CollUtil.isNotEmpty(refs)) {
                for (PaymentRefDO ref : refs) {
                    if (ref.getRefId() != null && ref.getAllocateAmount() != null) {
                        allocatedMap.merge(ref.getRefId(), ref.getAllocateAmount(), BigDecimal::add);
                    }
                }
            }
        }

        List<PaymentApplyRespVO> respList = pageResult.getList().stream().map(apply -> {
            BigDecimal allocatedAmount = allocatedMap.getOrDefault(apply.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            return convertToRespVO(apply, allocatedAmount);
        }).toList();

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public PaymentApplyRespVO getPaymentApply(Long id) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        List<PaymentRefDO> refs = paymentRefMapper.selectListByRefTypeAndRefId("PAY_APPLY", id);
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                if (ref.getAllocateAmount() != null) {
                    allocatedAmount = allocatedAmount.add(ref.getAllocateAmount());
                }
            }
        }

        return convertToRespVO(apply, allocatedAmount.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT_APPLY", subType = "CREATE", bizNo = "{{#_ret}}", success = "创建付款申请【{{#_ret}}】")
    public Long createPaymentApply(PaymentApplySaveReqVO createReqVO) {
        // 1. 金额基础校验
        if (createReqVO == null || createReqVO.getAmount() == null || createReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        String applyType = createReqVO.getApplyType();
        if (!PaymentApplyTypeEnum.isValid(applyType)) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        Long supplierId;
        String supplierName;
        Long reconId = null;
        String reconNo = null;
        String bankName = createReqVO.getBankName();
        String bankAccount = createReqVO.getBankAccount();

        if (PaymentApplyTypeEnum.RECON.getValue().equalsIgnoreCase(applyType)) {
            // 对账付款申请
            if (createReqVO.getReconId() == null) {
                throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
            }
            SupplierReconDO recon = supplierReconMapper.selectById(createReqVO.getReconId());
            if (recon == null) {
                throw exception(SUPPLIER_RECON_NOT_EXISTS);
            }
            if (!SupplierReconStatusEnum.CONFIRMED.getValue().equalsIgnoreCase(recon.getStatus())) {
                throw exception(PAYMENT_APPLY_RECON_NOT_CONFIRMED);
            }

            reconId = recon.getId();
            reconNo = recon.getReconNo();
            supplierId = recon.getSupplierId();
            supplierName = recon.getSupplierName();

            // 并发防超申守卫：重查该对账单已有有效申请额 (approvalStatus NOT IN ('REJECTED', 'CANCELLED'))
            List<PaymentApplyDO> existingApplies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                    .eq(PaymentApplyDO::getReconId, recon.getId())
                    .notIn(PaymentApplyDO::getApprovalStatus, Arrays.asList("REJECTED", "CANCELLED")));

            BigDecimal sumApplied = BigDecimal.ZERO;
            for (PaymentApplyDO exist : existingApplies) {
                if (exist.getAmount() != null) {
                    sumApplied = sumApplied.add(exist.getAmount());
                }
            }

            BigDecimal totalAfter = sumApplied.add(createReqVO.getAmount());
            if (recon.getTotalAmount() != null && totalAfter.compareTo(recon.getTotalAmount()) > 0) {
                throw exception(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED, totalAfter);
            }

        } else if (PaymentApplyTypeEnum.PREPAY.getValue().equalsIgnoreCase(applyType)) {
            // 预付付款申请
            if (createReqVO.getSupplierId() == null) {
                throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
            }
            SupplierDO supplier = supplierMapper.selectById(createReqVO.getSupplierId());
            if (supplier == null) {
                throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
            }

            if (!StringUtils.hasText(createReqVO.getPurpose())) {
                throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED, "预付付款申请用途不能为空");
            }

            supplierId = supplier.getId();
            supplierName = supplier.getCompanyName();
        } else {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        // 生成单号并保存主表
        String applyNo = generateApplyNoWithRetry();
        PaymentApplyDO apply = PaymentApplyDO.builder()
                .applyNo(applyNo)
                .applyType(applyType)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .reconId(reconId)
                .reconNo(reconNo)
                .amount(createReqVO.getAmount().setScale(2, RoundingMode.HALF_UP))
                .purpose(createReqVO.getPurpose())
                .bankName(bankName)
                .bankAccount(bankAccount)
                .approvalStatus("PENDING")
                .remark(createReqVO.getRemark())
                .build();
        paymentApplyMapper.insert(apply);

        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT_APPLY", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}", success = "修改付款申请【{{#updateReqVO.id}}】")
    public void updatePaymentApply(PaymentApplySaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        PaymentApplyDO apply = paymentApplyMapper.selectById(updateReqVO.getId());
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        String currentStatus = apply.getApprovalStatus();
        if (!("PENDING".equalsIgnoreCase(currentStatus)
                || "DRAFT".equalsIgnoreCase(currentStatus)
                || "REJECTED".equalsIgnoreCase(currentStatus))) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        if (updateReqVO.getAmount() == null || updateReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        if (PaymentApplyTypeEnum.RECON.getValue().equalsIgnoreCase(apply.getApplyType())) {
            if (apply.getReconId() != null) {
                SupplierReconDO recon = supplierReconMapper.selectById(apply.getReconId());
                if (recon != null) {
                    List<PaymentApplyDO> existingApplies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                            .eq(PaymentApplyDO::getReconId, recon.getId())
                            .ne(PaymentApplyDO::getId, apply.getId())
                            .notIn(PaymentApplyDO::getApprovalStatus, Arrays.asList("REJECTED", "CANCELLED")));

                    BigDecimal sumApplied = BigDecimal.ZERO;
                    for (PaymentApplyDO exist : existingApplies) {
                        if (exist.getAmount() != null) {
                            sumApplied = sumApplied.add(exist.getAmount());
                        }
                    }

                    BigDecimal totalAfter = sumApplied.add(updateReqVO.getAmount());
                    if (recon.getTotalAmount() != null && totalAfter.compareTo(recon.getTotalAmount()) > 0) {
                        throw exception(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED, totalAfter);
                    }
                }
            }
        } else if (PaymentApplyTypeEnum.PREPAY.getValue().equalsIgnoreCase(apply.getApplyType())) {
            if (!StringUtils.hasText(updateReqVO.getPurpose())) {
                throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED, "预付付款申请用途不能为空");
            }
            apply.setPurpose(updateReqVO.getPurpose());
        }

        apply.setAmount(updateReqVO.getAmount().setScale(2, RoundingMode.HALF_UP));
        if (updateReqVO.getRemark() != null) {
            apply.setRemark(updateReqVO.getRemark());
        }
        if (StringUtils.hasText(updateReqVO.getBankName())) {
            apply.setBankName(updateReqVO.getBankName());
        }
        if (StringUtils.hasText(updateReqVO.getBankAccount())) {
            apply.setBankAccount(updateReqVO.getBankAccount());
        }

        paymentApplyMapper.updateById(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT_APPLY", subType = "DELETE", bizNo = "{{#id}}", success = "删除付款申请【{{#id}}】")
    public void deletePaymentApply(Long id) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        String status = apply.getApprovalStatus();
        if (!("PENDING".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status))) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        paymentApplyMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT_APPLY", subType = "SUBMIT", bizNo = "{{#id}}", success = "提交付款申请审批【{{#id}}】")
    public Long submitPaymentApply(Long id) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        String status = apply.getApprovalStatus();
        if (!("PENDING".equalsIgnoreCase(status)
                || "DRAFT".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status))) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType("payment");
        submitReqVO.setBizId(id);
        submitReqVO.setComment("付款申请提交审批");
        Long instanceId = approvalInstanceService.submitInstance(submitReqVO);

        apply.setApprovalStatus("APPROVING");
        paymentApplyMapper.updateById(apply);

        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYMENT_APPLY", subType = "CANCEL_APPROVE", bizNo = "{{#id}}", success = "撤回付款申请审批【{{#id}}】")
    public void cancelApprovePaymentApply(Long id) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }

        if (!"APPROVING".equalsIgnoreCase(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceRespVO latestInstance = approvalInstanceService.getLatestInstanceByBiz("payment", id);
        if (latestInstance != null && latestInstance.getId() != null) {
            ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
            withdrawReqVO.setReason("撤回付款申请审批");
            approvalInstanceService.withdrawInstance(latestInstance.getId(), withdrawReqVO);
        }

        apply.setApprovalStatus("PENDING");
        paymentApplyMapper.updateById(apply);
    }

    @Override
    public BigDecimal getPrepayBalance(Long supplierId) {
        if (supplierId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 1. 查询该供应商下所有 PREPAY 类型的付款申请
        List<PaymentApplyDO> prepayApplies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getSupplierId, supplierId)
                .eq(PaymentApplyDO::getApplyType, PaymentApplyTypeEnum.PREPAY.getValue()));

        BigDecimal allocatedAmountSum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(prepayApplies)) {
            List<Long> applyIds = prepayApplies.stream().map(PaymentApplyDO::getId).filter(Objects::nonNull).toList();
            if (CollUtil.isNotEmpty(applyIds)) {
                List<PaymentRefDO> refs = paymentRefMapper.selectListByRefTypeAndRefIds("PAY_APPLY", applyIds);
                if (CollUtil.isNotEmpty(refs)) {
                    for (PaymentRefDO ref : refs) {
                        if (ref.getAllocateAmount() != null) {
                            allocatedAmountSum = allocatedAmountSum.add(ref.getAllocateAmount());
                        }
                    }
                }
            }
        }

        // 2. 查询该供应商下所有 direction = 'OUT' AND status = 'COMPLETED' 的 payment
        List<PaymentDO> payments = paymentMapper.selectList(new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getSupplierId, supplierId)
                .eq(PaymentDO::getDirection, PaymentDirectionEnum.OUT.getValue())
                .eq(PaymentDO::getStatus, PaymentStatusEnum.COMPLETED.getValue()));

        BigDecimal prepayUsedSum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(payments)) {
            for (PaymentDO payment : payments) {
                if (payment.getPrepayUsedAmount() != null) {
                    prepayUsedSum = prepayUsedSum.add(payment.getPrepayUsedAmount());
                }
            }
        }

        return allocatedAmountSum.subtract(prepayUsedSum).setScale(2, RoundingMode.HALF_UP);
    }

    private PaymentApplyRespVO convertToRespVO(PaymentApplyDO apply, BigDecimal allocatedAmount) {
        return PaymentApplyRespVO.builder()
                .id(apply.getId())
                .applyNo(apply.getApplyNo())
                .applyType(apply.getApplyType())
                .supplierId(apply.getSupplierId())
                .supplierName(apply.getSupplierName())
                .reconId(apply.getReconId())
                .reconNo(apply.getReconNo())
                .amount(apply.getAmount())
                .purpose(apply.getPurpose())
                .bankName(apply.getBankName())
                .bankAccount(apply.getBankAccount())
                .approvalStatus(apply.getApprovalStatus())
                .remark(apply.getRemark())
                .allocatedAmount(allocatedAmount)
                .createTime(apply.getCreateTime())
                .build();
    }

    /**
     * 生成付款申请单号：FK + 8位年月日 + 3位日流水（超 999 自然扩位）
     */
    private String generateApplyNo() {
        LocalDate today = LocalDate.now();
        String prefix = APPLY_NO_PREFIX + today.format(DATE_FORMATTER);
        List<PaymentApplyDO> candidates = paymentApplyMapper.selectListByApplyNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        if (CollUtil.isNotEmpty(candidates)) {
            for (PaymentApplyDO candidate : candidates) {
                String no = candidate.getApplyNo();
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
    private String generateApplyNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateApplyNo();
            if (paymentApplyMapper.selectByApplyNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
    }

}

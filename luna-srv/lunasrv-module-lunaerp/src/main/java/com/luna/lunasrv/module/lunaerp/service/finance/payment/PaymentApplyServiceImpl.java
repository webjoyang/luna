package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyType;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReconStatus;
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

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 付款申请 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PaymentApplyServiceImpl implements PaymentApplyService {

    private static final String APPLY_NO_PREFIX = "FK";
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 50;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaymentApply(PaymentApplyCreateReqVO createReqVO) {
        // 1. 金额守卫
        if (createReqVO.getAmount() == null || createReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        Long supplierId;
        String supplierName;
        Long reconId = null;
        String reconNo = null;
        String bankName = createReqVO.getBankName();
        String bankAccount = createReqVO.getBankAccount();

        // 2. 类型分支处理
        // 2. 类型分支处理
        if (ApplyType.RECON.getValue().equalsIgnoreCase(createReqVO.getApplyType())) {
            SupplierReconDO recon = validateAndLockRecon(createReqVO.getReconId(), null, createReqVO.getAmount());
            supplierId = recon.getSupplierId();
            supplierName = recon.getSupplierName();
            reconId = recon.getId();
            reconNo = recon.getReconNo();
        } else if (ApplyType.PREPAY.getValue().equalsIgnoreCase(createReqVO.getApplyType())) {
            // PREPAY：purpose 必填、供应商必填且存在
            if (StrUtil.isBlank(createReqVO.getPurpose())) {
                throw invalidParamException("预付用途不能为空");
            }
            if (createReqVO.getSupplierId() == null) {
                throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
            }
            SupplierDO supplier = supplierMapper.selectById(createReqVO.getSupplierId());
            if (supplier == null) {
                throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
            }
            validateTenant(supplier.getTenantId());

            supplierId = supplier.getId();
            supplierName = supplier.getCompanyName();
        } else {
            throw invalidParamException("不支持的申请类型【{}】", createReqVO.getApplyType());
        }

        // 3. ADR-0006 按天取号
        String applyNo = generateApplyNo(LocalDate.now());

        // 4. 持久化
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setApplyNo(applyNo);
        apply.setApplyType(createReqVO.getApplyType());
        apply.setSupplierId(supplierId);
        apply.setSupplierName(supplierName);
        apply.setReconId(reconId);
        apply.setReconNo(reconNo);
        apply.setAmount(createReqVO.getAmount().setScale(2, RoundingMode.HALF_UP));
        apply.setPurpose(createReqVO.getPurpose());
        apply.setBankName(bankName);
        apply.setBankAccount(bankAccount);
        apply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        apply.setRemark(createReqVO.getRemark());

        paymentApplyMapper.insert(apply);
        log.info("[createPaymentApply] 创建付款申请成功: id={}, applyNo={}, type={}", apply.getId(), apply.getApplyNo(), apply.getApplyType());
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentApply(PaymentApplyUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        PaymentApplyDO apply = paymentApplyMapper.selectById(updateReqVO.getId());
        validateApplyExistsAndTenant(apply);

        // 状态守卫：仅 DRAFT/PENDING/REJECTED 可修改
        if (!isDraftOrRejected(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        // 金额守卫
        if (updateReqVO.getAmount() == null || updateReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYMENT_AMOUNT_INVALID);
        }

        // RECON 超额重验
        if (ApplyType.RECON.getValue().equalsIgnoreCase(apply.getApplyType())) {
            validateAndLockRecon(apply.getReconId(), apply.getId(), updateReqVO.getAmount());
        } else if (ApplyType.PREPAY.getValue().equalsIgnoreCase(apply.getApplyType())) {
            if (StrUtil.isBlank(updateReqVO.getPurpose())) {
                throw invalidParamException("预付用途不能为空");
            }
        }

        apply.setAmount(updateReqVO.getAmount().setScale(2, RoundingMode.HALF_UP));
        if (updateReqVO.getPurpose() != null) {
            apply.setPurpose(updateReqVO.getPurpose());
        }
        if (updateReqVO.getRemark() != null) {
            apply.setRemark(updateReqVO.getRemark());
        }

        paymentApplyMapper.updateById(apply);
        log.info("[updatePaymentApply] 修改付款申请成功: id={}", apply.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaymentApply(Long id) {
        if (id == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        validateApplyExistsAndTenant(apply);

        // 状态守卫：仅 DRAFT/PENDING 可删
        if (!isDraft(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        paymentApplyMapper.deleteById(id);
        log.info("[deletePaymentApply] 删除付款申请软删进回收站: id={}", id);
    }

    @Override
    public PaymentApplyRespVO getPaymentApply(Long id) {
        if (id == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        validateApplyExistsAndTenant(apply);

        PaymentApplyRespVO vo = BeanUtils.toBean(apply, PaymentApplyRespVO.class);
        BigDecimal allocated = paymentRefMapper.selectSumAllocatedAmountByApplyId(id);
        if (allocated == null) {
            allocated = BigDecimal.ZERO;
        }
        allocated = allocated.setScale(2, RoundingMode.HALF_UP);
        vo.setAllocatedAmount(allocated);
        vo.setRemainAmount(apply.getAmount().subtract(allocated).setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public PageResult<PaymentApplyRespVO> getPaymentApplyPage(PaymentApplyPageReqVO pageReqVO) {
        LambdaQueryWrapperX<PaymentApplyDO> queryWrapper = new LambdaQueryWrapperX<PaymentApplyDO>()
                .eqIfPresent(PaymentApplyDO::getSupplierId, pageReqVO.getSupplierId())
                .eqIfPresent(PaymentApplyDO::getApplyType, pageReqVO.getApplyType())
                .eqIfPresent(PaymentApplyDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .and(StrUtil.isNotBlank(pageReqVO.getKeyword()), w -> w
                        .like(PaymentApplyDO::getApplyNo, pageReqVO.getKeyword())
                        .or().like(PaymentApplyDO::getSupplierName, pageReqVO.getKeyword())
                        .or().like(PaymentApplyDO::getReconNo, pageReqVO.getKeyword())
                        .or().like(PaymentApplyDO::getPurpose, pageReqVO.getKeyword()))
                .orderByDesc(PaymentApplyDO::getId);

        PageResult<PaymentApplyDO> pageResult = paymentApplyMapper.selectPage(pageReqVO, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 防 N+1 批量快照查询已核销金额
        List<Long> applyIds = CollectionUtils.convertList(pageResult.getList(), PaymentApplyDO::getId);
        Map<Long, BigDecimal> allocatedMap = batchLoadAllocatedMap(applyIds);

        List<PaymentApplyRespVO> respList = new ArrayList<>();
        for (PaymentApplyDO apply : pageResult.getList()) {
            PaymentApplyRespVO vo = BeanUtils.toBean(apply, PaymentApplyRespVO.class);
            BigDecimal allocated = allocatedMap.getOrDefault(apply.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            vo.setAllocatedAmount(allocated);
            vo.setRemainAmount(apply.getAmount().subtract(allocated).setScale(2, RoundingMode.HALF_UP));
            respList.add(vo);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_PAYMENT", subType = "SUBMIT_APPLY", bizNo = "{{#id}}", success = "提交付款申请【{{#id}}】审批")
    public void submitPaymentApply(Long id) {
        if (id == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        validateApplyExistsAndTenant(apply);

        // 状态守卫：仅 DRAFT/PENDING/REJECTED 可提交
        if (!isDraftOrRejected(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        // RECON 再次校验对账单状态与金额上限
        if (ApplyType.RECON.getValue().equalsIgnoreCase(apply.getApplyType())) {
            validateAndLockRecon(apply.getReconId(), apply.getId(), apply.getAmount());
        } else if (ApplyType.PREPAY.getValue().equalsIgnoreCase(apply.getApplyType())) {
            if (StrUtil.isBlank(apply.getPurpose())) {
                throw invalidParamException("预付用途不能为空");
            }
        }

        // 调用 02 号审批引擎发起实例
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType(ApprovalHandler.BIZ_TYPE_PAYMENT);
        submitReqVO.setBizId(id);
        submitReqVO.setComment("提交付款申请审批");

        approvalInstanceService.submitInstance(submitReqVO);
        log.info("[submitPaymentApply] 付款申请提交审批成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_PAYMENT", subType = "CANCEL_APPLY", bizNo = "{{#id}}", success = "撤回付款申请【{{#id}}】审批")
    public void cancelApprove(Long id) {
        if (id == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        PaymentApplyDO apply = paymentApplyMapper.selectById(id);
        validateApplyExistsAndTenant(apply);

        // 仅 APPROVING 可撤回
        if (!ApplyStatus.APPROVING.getValue().equalsIgnoreCase(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(ApprovalHandler.BIZ_TYPE_PAYMENT, id);
        if (instance == null || !ApplyStatus.APPROVING.getValue().equalsIgnoreCase(instance.getStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
        withdrawReqVO.setReason("发起人撤回付款申请审批");
        approvalInstanceService.withdrawInstance(instance.getId(), withdrawReqVO);
        log.info("[cancelApprove] 付款申请审批撤回成功: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    public BigDecimal getPrepayBalance(Long supplierId) {
        if (supplierId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. Σ(PREPAY 类申请被核销的 allocate_amount)
        BigDecimal prepayAllocated = paymentApplyMapper.selectSumAllocatedAmountBySupplierAndType(
                supplierId, ApplyType.PREPAY.getValue(), tenantId);
        if (prepayAllocated == null) {
            prepayAllocated = BigDecimal.ZERO;
        }

        // 2. Σ(所有付款登记 prepay_used_amount)
        BigDecimal prepayUsed = paymentMapper.selectSumPrepayUsedAmountBySupplier(supplierId, tenantId);
        if (prepayUsed == null) {
            prepayUsed = BigDecimal.ZERO;
        }

        BigDecimal balance = prepayAllocated.subtract(prepayUsed);
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 私有辅助方法 ====================

    private BigDecimal computeReconAppliedSum(Long reconId, Long excludeApplyId) {
        List<PaymentApplyDO> applies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getReconId, reconId)
                .notIn(PaymentApplyDO::getApprovalStatus, List.of(ApplyStatus.REJECTED.getValue(), ApplyStatus.CANCELLED.getValue())));
        if (CollUtil.isEmpty(applies)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentApplyDO a : applies) {
            if (excludeApplyId != null && excludeApplyId.equals(a.getId())) {
                continue;
            }
            if (a.getAmount() != null) {
                sum = sum.add(a.getAmount());
            }
        }
        return sum;
    }

    private SupplierReconDO validateAndLockRecon(Long reconId, Long excludeApplyId, BigDecimal amount) {
        if (reconId == null) {
            throw exception(PAYMENT_APPLY_RECON_NOT_CONFIRMED);
        }
        SupplierReconDO recon = supplierReconMapper.selectOneForUpdate(SupplierReconDO::getId, reconId);
        if (recon == null) {
            recon = supplierReconMapper.selectById(reconId);
        }
        if (recon == null || !ReconStatus.CONFIRMED.getValue().equalsIgnoreCase(recon.getStatus())) {
            throw exception(PAYMENT_APPLY_RECON_NOT_CONFIRMED);
        }
        validateTenant(recon.getTenantId());

        BigDecimal appliedSum = computeReconAppliedSum(recon.getId(), excludeApplyId);
        BigDecimal newTotal = appliedSum.add(amount != null ? amount : BigDecimal.ZERO);
        if (newTotal.compareTo(recon.getTotalAmount()) > 0) {
            throw exception(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED, newTotal.setScale(2, RoundingMode.HALF_UP));
        }
        return recon;
    }

    private Map<Long, BigDecimal> batchLoadAllocatedMap(Collection<Long> applyIds) {
        if (CollUtil.isEmpty(applyIds)) {
            return Collections.emptyMap();
        }
        List<PaymentRefDO> refs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getRefType, "PAY_APPLY")
                .inOrEmpty(PaymentRefDO::getRefId, applyIds));
        Map<Long, BigDecimal> allocatedMap = new HashMap<>();
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                if (ref.getRefId() != null && ref.getAllocateAmount() != null) {
                    allocatedMap.merge(ref.getRefId(), ref.getAllocateAmount(), BigDecimal::add);
                }
            }
        }
        return allocatedMap;
    }

    private boolean isDraft(String status) {
        return ApplyStatus.isDraft(status);
    }

    private boolean isDraftOrRejected(String status) {
        return isDraft(status) || ApplyStatus.REJECTED.getValue().equalsIgnoreCase(status);
    }

    private void validateApplyExistsAndTenant(PaymentApplyDO apply) {
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        validateTenant(apply.getTenantId());
    }

    private void validateTenant(Long tenantId) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && tenantId != null && !currentTenantId.equals(tenantId)) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
    }

    private String generateApplyNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = APPLY_NO_PREFIX + DATE_FORMATTER.format(targetDate);
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
                try {
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignore) {
                    continue;
                }
            }
        }
        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            long seq = maxNum + i;
            String candidate = prefix + String.format("%0" + maxDigits + "d", seq);
            if (paymentApplyMapper.selectByApplyNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(PAYMENT_APPLY_NO_EXHAUSTED);
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.PayrollItemKindEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PayrollPaymentStatusEnum;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 员工工资单业务服务实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    public static final String PAYROLL_NO_PREFIX = "GZ";
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 20;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private PayrollMapper payrollMapper;

    @Resource
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @Resource
    private PayrollLineItemMapper payrollLineItemMapper;

    @Resource
    private PayrollDefectRefMapper payrollDefectRefMapper;

    @Resource
    private PayrollPaymentMapper payrollPaymentMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Resource
    private PayrollDefectService payrollDefectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "CREATE", bizNo = "{{#result}}",
            success = "创建工资单【{{#result}}】")
    public Long createPayroll(PayrollCreateReqVO createReqVO) {
        if (createReqVO == null || StrUtil.isBlank(createReqVO.getPeriod())) {
            throw exception(PAYROLL_PERIOD_DUPLICATE);
        }
        String period = createReqVO.getPeriod().trim();

        // 1. 同租户同期间有效工资单唯一性检查（应用层守卫）
        validatePeriodNotDuplicate(period, null);

        // 2. 缺省标题
        String title = StrUtil.isNotBlank(createReqVO.getTitle())
                ? createReqVO.getTitle().trim()
                : period + " 工资表";

        // 3. 构建主实体
        PayrollDO payroll = new PayrollDO();
        payroll.setPeriod(period);
        payroll.setTitle(title);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        payroll.setRemark(createReqVO.getRemark());
        payroll.setEarningTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payroll.setDeductionTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payroll.setNetAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        // 4. 若传入员工行，进行明细校验与金额权威重算
        List<PayrollEmployeeSaveReqVO> employees = createReqVO.getEmployees();
        List<CalculatedEmployeeData> calculatedEmployees = null;
        if (CollUtil.isNotEmpty(employees)) {
            calculatedEmployees = validateAndCalculateEmployees(employees);
            recalculatePayrollTotals(payroll, calculatedEmployees);

            // 4.1 次品引用指纹与状态前置校验
            List<PayrollDefectRefSaveReqVO> allDefectRefs = extractDefectRefs(calculatedEmployees);
            if (CollUtil.isNotEmpty(allDefectRefs)) {
                payrollDefectService.validateDefectRefsForSave(null, allDefectRefs);
            }
        }

        // 5. 生成单号并持久化（支持数据库部分唯一索引兜底防并发）
        saveWithGeneratedNo(payroll);

        // 6. 保存员工汇总行与款项明细行
        if (CollUtil.isNotEmpty(calculatedEmployees)) {
            saveEmployeeAndItemRows(payroll.getId(), calculatedEmployees);
        }

        log.info("[createPayroll] 创建工资单成功: id={}, payrollNo={}, period={}, netAmount={}",
                payroll.getId(), payroll.getPayrollNo(), payroll.getPeriod(), payroll.getNetAmount());
        return payroll.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}",
            success = "更新工资单【{{#updateReqVO.id}}】")
    public void updatePayroll(PayrollUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }

        // 1. 查验工资单存在与租户安全
        PayrollDO payroll = validatePayrollExistsAndTenant(updateReqVO.getId());

        // 2. 状态守卫：仅 PENDING / REJECTED 可修改
        if (!isPendingOrRejected(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        // 3. 员工行守卫：至少1人（025_009）
        List<PayrollEmployeeSaveReqVO> employees = updateReqVO.getEmployees();
        if (CollUtil.isEmpty(employees)) {
            throw exception(PAYROLL_EMPTY);
        }

        // 4. 明细校验与金额权威重算（全员实发非负 025_011、员工不重复 025_003、用户有效 025_012、款项合法 025_004）
        List<CalculatedEmployeeData> calculatedEmployees = validateAndCalculateEmployees(employees);
        recalculatePayrollTotals(payroll, calculatedEmployees);

        // 4.1 校验次品引用指纹与状态（并发重查候选）
        List<PayrollDefectRefSaveReqVO> allDefectRefs = extractDefectRefs(calculatedEmployees);
        if (CollUtil.isNotEmpty(allDefectRefs)) {
            payrollDefectService.validateDefectRefsForSave(payroll.getId(), allDefectRefs);
        }

        // 5. 更新标题与备注
        if (StrUtil.isNotBlank(updateReqVO.getTitle())) {
            payroll.setTitle(updateReqVO.getTitle().trim());
        }
        payroll.setRemark(updateReqVO.getRemark());
        payrollMapper.updateById(payroll);

        // 6. 整体替换员工行与款项行（同时清理旧次品引用，连 refs 删后立即释放）
        List<PayrollLineItemDO> oldLineItems = payrollLineItemMapper.selectList(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, payroll.getId()));
        if (CollUtil.isNotEmpty(oldLineItems)) {
            List<Long> oldLineItemIds = oldLineItems.stream().map(PayrollLineItemDO::getId).collect(Collectors.toList());
            payrollDefectRefMapper.delete(new LambdaQueryWrapperX<PayrollDefectRefDO>()
                    .inOrEmpty(PayrollDefectRefDO::getLineItemId, oldLineItemIds));
        }
        payrollLineItemMapper.delete(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, payroll.getId()));
        payrollEmployeeMapper.delete(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, payroll.getId()));

        saveEmployeeAndItemRows(payroll.getId(), calculatedEmployees);

        log.info("[updatePayroll] 更新工资单成功: id={}, payrollNo={}, netAmount={}",
                payroll.getId(), payroll.getPayrollNo(), payroll.getNetAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "DELETE", bizNo = "{{#id}}",
            success = "删除草稿工资单【{{#id}}】")
    public void deletePayroll(Long id) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);

        // 仅 PENDING / REJECTED 草稿可删除
        if (!isPendingOrRejected(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        // 软删除主表及下属子表（含次品引用）
        List<PayrollLineItemDO> lineItems = payrollLineItemMapper.selectList(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, id));
        if (CollUtil.isNotEmpty(lineItems)) {
            List<Long> lineItemIds = lineItems.stream().map(PayrollLineItemDO::getId).collect(Collectors.toList());
            payrollDefectRefMapper.delete(new LambdaQueryWrapperX<PayrollDefectRefDO>()
                    .inOrEmpty(PayrollDefectRefDO::getLineItemId, lineItemIds));
        }
        payrollLineItemMapper.delete(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, id));
        payrollEmployeeMapper.delete(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, id));
        payrollMapper.deleteById(id);

        log.info("[deletePayroll] 草稿工资单已删除入回收站: id={}, payrollNo={}", id, payroll.getPayrollNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "SUBMIT", bizNo = "{{#id}}",
            success = "提交工资单【{{#id}}】审批")
    public void submitPayroll(Long id) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);

        // 1. 状态守卫：仅 PENDING / REJECTED 可提交
        if (!isPendingOrRejected(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        // 2. 员工行守卫：≥1 行（025_009）
        List<PayrollEmployeeDO> employees = payrollEmployeeMapper.selectList(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, id));
        if (CollUtil.isEmpty(employees)) {
            throw exception(PAYROLL_EMPTY);
        }

        // 3. 期间唯一守卫（025_002）
        validatePeriodNotDuplicate(payroll.getPeriod(), id);

        // 4. 全员实发非负守卫（025_011）
        for (PayrollEmployeeDO emp : employees) {
            if (emp.getNetAmount() == null || emp.getNetAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PAYROLL_NET_AMOUNT_NEGATIVE);
            }
        }

        // 5. 提交时重验 refs（025_005，本票以空集天然通过）
        validateDefectRefs(id);

        // 6. 更新提交时间
        payroll.setSubmitTime(LocalDateTime.now());
        payrollMapper.updateById(payroll);

        // 7. 发起 02 号审批流实例（biz_type = payroll）
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType(ApprovalHandler.BIZ_TYPE_PAYROLL);
        submitReqVO.setBizId(id);
        submitReqVO.setComment("提交员工工资单审批");
        approvalInstanceService.submitInstance(submitReqVO);

        log.info("[submitPayroll] 工资单提交审批成功: id={}, payrollNo={}", id, payroll.getPayrollNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "CANCEL_APPROVE", bizNo = "{{#id}}",
            success = "撤回工资单【{{#id}}】审批")
    public void cancelApprove(Long id) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);

        // 仅 APPROVING 审批中可撤回
        if (!ApplyStatus.APPROVING.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(ApprovalHandler.BIZ_TYPE_PAYROLL, id);
        if (instance == null || !ApplyStatus.APPROVING.getValue().equalsIgnoreCase(instance.getStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
        withdrawReqVO.setReason("发起人撤回员工工资单审批");
        approvalInstanceService.withdrawInstance(instance.getId(), withdrawReqVO);

        log.info("[cancelApprove] 工资单审批已撤回: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PAYROLL", subType = "CANCEL", bizNo = "{{#id}}",
            success = "作废工资单【{{#id}}】")
    public void cancelPayroll(Long id, String cancelReason) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        if (StrUtil.isBlank(cancelReason)) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);

        // 1. 状态守卫：仅 APPROVED 可作废
        if (!ApplyStatus.APPROVED.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        // 2. 发放记录守卫：发放批次=0，有发放记录禁止作废（025_006）
        Long paymentCount = payrollPaymentMapper.selectCount(new LambdaQueryWrapperX<PayrollPaymentDO>()
                .eq(PayrollPaymentDO::getPayrollId, id));
        if (paymentCount != null && paymentCount > 0) {
            throw exception(PAYROLL_HAS_PAYMENT_NO_CANCEL);
        }

        // 3. 置状态为 CANCELLED，记录留痕，审批历史记录保留
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        payroll.setApprovalStatus("CANCELLED");
        payroll.setCancelledBy(loginUserId != null ? String.valueOf(loginUserId) : "system");
        payroll.setCancelledAt(LocalDateTime.now());
        payroll.setCancelReason(cancelReason.trim());
        payrollMapper.updateById(payroll);

        log.info("[cancelPayroll] 工资单已作废: id={}, payrollNo={}, reason={}", id, payroll.getPayrollNo(), cancelReason);
    }

    @Override
    public PayrollRespVO getPayroll(Long id) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);
        PayrollRespVO respVO = BeanUtils.toBean(payroll, PayrollRespVO.class);

        // 1. 员工行与款项行组装
        List<PayrollEmployeeDO> empDOs = payrollEmployeeMapper.selectList(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, id)
                .orderByAsc(PayrollEmployeeDO::getId));

        List<PayrollLineItemDO> lineItemDOs = payrollLineItemMapper.selectList(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, id)
                .orderByAsc(PayrollLineItemDO::getId));

        // 次品引用（07 票使用，本票预先挂接结构）
        List<Long> lineItemIds = lineItemDOs.stream().map(PayrollLineItemDO::getId).collect(Collectors.toList());
        List<PayrollDefectRefDO> defectRefs = CollUtil.isNotEmpty(lineItemIds)
                ? payrollDefectRefMapper.selectList(new LambdaQueryWrapperX<PayrollDefectRefDO>()
                .inOrEmpty(PayrollDefectRefDO::getLineItemId, lineItemIds))
                : Collections.emptyList();
        Map<Long, List<PayrollDefectRefDO>> defectMap = defectRefs.stream()
                .collect(Collectors.groupingBy(PayrollDefectRefDO::getLineItemId));

        Map<Long, List<PayrollLineItemDO>> lineItemMap = lineItemDOs.stream()
                .collect(Collectors.groupingBy(PayrollLineItemDO::getEmployeeRowId));

        List<PayrollEmployeeRespVO> employeeRespVOs = new ArrayList<>();
        for (PayrollEmployeeDO empDO : empDOs) {
            PayrollEmployeeRespVO empVO = BeanUtils.toBean(empDO, PayrollEmployeeRespVO.class);
            List<PayrollLineItemDO> empItems = lineItemMap.getOrDefault(empDO.getId(), Collections.emptyList());
            List<PayrollLineItemRespVO> itemVOs = new ArrayList<>();
            for (PayrollLineItemDO itemDO : empItems) {
                PayrollLineItemRespVO itemVO = BeanUtils.toBean(itemDO, PayrollLineItemRespVO.class);
                List<PayrollDefectRefDO> itemRefs = defectMap.getOrDefault(itemDO.getId(), Collections.emptyList());
                itemVO.setDefectRefs(BeanUtils.toBean(itemRefs, PayrollDefectRefRespVO.class));
                itemVOs.add(itemVO);
            }
            empVO.setItems(itemVOs);
            employeeRespVOs.add(empVO);
        }
        respVO.setEmployees(employeeRespVOs);

        // 2. 发放批次与聚合金额
        List<PayrollPaymentDO> payments = payrollPaymentMapper.selectList(new LambdaQueryWrapperX<PayrollPaymentDO>()
                .eq(PayrollPaymentDO::getPayrollId, id)
                .orderByDesc(PayrollPaymentDO::getId));
        respVO.setPayments(BeanUtils.toBean(payments, PayrollPaymentRespVO.class));

        BigDecimal paidAmount = payments.stream()
                .map(PayrollPaymentDO::getOriginalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        respVO.setPaidAmount(paidAmount);
        respVO.setPaymentStatus(resolvePaymentStatus(paidAmount, payroll.getNetAmount()));

        // 3. 审批进度摘要
        try {
            ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(ApprovalHandler.BIZ_TYPE_PAYROLL, id);
            respVO.setApprovalProgress(instance);
        } catch (Exception e) {
            log.warn("[getPayroll] 获取审批进度摘要异常: id={}, error={}", id, e.getMessage());
        }

        return respVO;
    }

    @Override
    public PageResult<PayrollRespVO> getPayrollPage(PayrollPageReqVO pageReqVO) {
        LambdaQueryWrapperX<PayrollDO> queryWrapper = new LambdaQueryWrapperX<PayrollDO>()
                .likeIfPresent(PayrollDO::getPayrollNo, pageReqVO.getPayrollNo())
                .eqIfPresent(PayrollDO::getPeriod, pageReqVO.getPeriod())
                .eqIfPresent(PayrollDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .orderByDesc(PayrollDO::getPeriod)
                .orderByDesc(PayrollDO::getId);

        // 派生发放态筛选：通过发放批次金额子查询
        if (StrUtil.isNotBlank(pageReqVO.getPaymentStatus())) {
            String pStatus = pageReqVO.getPaymentStatus().trim().toUpperCase();
            if (PayrollPaymentStatusEnum.UNPAID.getValue().equals(pStatus)) {
                queryWrapper.apply("(SELECT COALESCE(SUM(original_amount), 0) FROM lunaerp_finance_payroll_payment p "
                        + "WHERE p.payroll_id = lunaerp_finance_payroll.id AND p.deleted = 0) = 0");
            } else if (PayrollPaymentStatusEnum.PARTIAL.getValue().equals(pStatus)) {
                queryWrapper.apply("(SELECT COALESCE(SUM(original_amount), 0) FROM lunaerp_finance_payroll_payment p "
                        + "WHERE p.payroll_id = lunaerp_finance_payroll.id AND p.deleted = 0) > 0 "
                        + "AND (SELECT COALESCE(SUM(original_amount), 0) FROM lunaerp_finance_payroll_payment p "
                        + "WHERE p.payroll_id = lunaerp_finance_payroll.id AND p.deleted = 0) < lunaerp_finance_payroll.net_amount");
            } else if (PayrollPaymentStatusEnum.PAID.getValue().equals(pStatus)) {
                queryWrapper.apply("(SELECT COALESCE(SUM(original_amount), 0) FROM lunaerp_finance_payroll_payment p "
                        + "WHERE p.payroll_id = lunaerp_finance_payroll.id AND p.deleted = 0) >= lunaerp_finance_payroll.net_amount "
                        + "AND lunaerp_finance_payroll.net_amount > 0");
            }
        }

        PageResult<PayrollDO> pageResult = payrollMapper.selectPage(pageReqVO, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询当前页发放金额并计算派生状态（遵循 coding-standards.md §2 batch* 规范）
        List<Long> payrollIds = pageResult.getList().stream().map(PayrollDO::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> paidAmountMap = batchGetPaidAmountMap(payrollIds);

        List<PayrollRespVO> respList = new ArrayList<>();
        for (PayrollDO payroll : pageResult.getList()) {
            PayrollRespVO vo = BeanUtils.toBean(payroll, PayrollRespVO.class);
            BigDecimal paidAmount = paidAmountMap.getOrDefault(payroll.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            vo.setPaidAmount(paidAmount);
            vo.setPaymentStatus(resolvePaymentStatus(paidAmount, payroll.getNetAmount()));
            respList.add(vo);
        }

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @LogRecord(type = "PAYROLL", subType = "PRINT", bizNo = "{{#id}}",
            success = "获取工资单【{{#id}}】A4套打数据")
    public PayrollPrintRespVO getPayrollPrintData(Long id) {
        if (id == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        PayrollDO payroll = validatePayrollExistsAndTenant(id);

        // 仅限 APPROVED 终态可取 A4 套打数据
        if (!ApplyStatus.APPROVED.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        List<PayrollEmployeeDO> employees = payrollEmployeeMapper.selectList(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, id)
                .orderByAsc(PayrollEmployeeDO::getId));

        List<PayrollPrintEmployeeRespVO> printItems = new ArrayList<>();
        int lineNo = 1;
        for (PayrollEmployeeDO emp : employees) {
            printItems.add(PayrollPrintEmployeeRespVO.builder()
                    .lineNo(lineNo++)
                    .userId(emp.getUserId())
                    .nickname(emp.getNickname())
                    .earningTotal(emp.getEarningTotal())
                    .deductionTotal(emp.getDeductionTotal())
                    .netAmount(emp.getNetAmount())
                    .signature(emp.getNickname())
                    .remark(emp.getRemark())
                    .build());
        }

        return PayrollPrintRespVO.builder()
                .bizType("PAYROLL")
                .title(StrUtil.isNotBlank(payroll.getTitle()) ? payroll.getTitle() : payroll.getPeriod() + " 工资表")
                .id(payroll.getId())
                .payrollNo(payroll.getPayrollNo())
                .period(payroll.getPeriod())
                .approvalStatus(payroll.getApprovalStatus())
                .earningTotal(payroll.getEarningTotal())
                .deductionTotal(payroll.getDeductionTotal())
                .netAmount(payroll.getNetAmount())
                .submitTime(payroll.getSubmitTime())
                .approveTime(payroll.getApproveTime())
                .remark(payroll.getRemark())
                .printTime(LocalDateTime.now())
                .items(printItems)
                .build();
    }

    // =========================================================================
    // 内部私有辅助逻辑
    // =========================================================================

    private PayrollDO validatePayrollExistsAndTenant(Long id) {
        PayrollDO payroll = payrollMapper.selectById(id);
        if (payroll == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && payroll.getTenantId() != null && !tenantId.equals(payroll.getTenantId())) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        return payroll;
    }

    private void validatePeriodNotDuplicate(String period, Long excludeId) {
        Long count = payrollMapper.selectCount(new LambdaQueryWrapperX<PayrollDO>()
                .eq(PayrollDO::getPeriod, period)
                .ne(PayrollDO::getApprovalStatus, "CANCELLED")
                .ne(excludeId != null, PayrollDO::getId, excludeId));
        if (count != null && count > 0) {
            throw exception(PAYROLL_PERIOD_DUPLICATE);
        }
    }

    private void validateDefectRefs(Long payrollId) {
        payrollDefectService.validateDefectRefsForSubmit(payrollId);
        log.debug("[validateDefectRefs] 工资单 refs 校验通过: payrollId={}", payrollId);
    }

    private boolean isPendingOrRejected(String status) {
        return ApplyStatus.isDraft(status)
                || ApplyStatus.REJECTED.getValue().equalsIgnoreCase(status);
    }

    private String resolvePaymentStatus(BigDecimal paidAmount, BigDecimal netAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PayrollPaymentStatusEnum.UNPAID.getValue();
        }
        if (netAmount != null && paidAmount.compareTo(netAmount) >= 0) {
            return PayrollPaymentStatusEnum.PAID.getValue();
        }
        return PayrollPaymentStatusEnum.PARTIAL.getValue();
    }

    /**
     * 批量查询发放金额（遵循 coding-standards.md §2 batch* 规范）
     */
    private Map<Long, BigDecimal> batchGetPaidAmountMap(Collection<Long> payrollIds) {
        if (CollUtil.isEmpty(payrollIds)) {
            return Collections.emptyMap();
        }
        List<PayrollPaymentDO> payments = payrollPaymentMapper.selectList(new LambdaQueryWrapperX<PayrollPaymentDO>()
                .inOrEmpty(PayrollPaymentDO::getPayrollId, payrollIds));
        Map<Long, BigDecimal> map = new HashMap<>();
        for (PayrollPaymentDO payment : payments) {
            if (payment.getOriginalAmount() != null) {
                map.merge(payment.getPayrollId(), payment.getOriginalAmount(), BigDecimal::add);
            }
        }
        return map;
    }

    /**
     * 校验员工行与款项行，并执行权威计算
     */
    private List<CalculatedEmployeeData> validateAndCalculateEmployees(List<PayrollEmployeeSaveReqVO> employees) {
        Set<Long> userIds = new HashSet<>();
        for (PayrollEmployeeSaveReqVO emp : employees) {
            if (emp.getUserId() == null) {
                throw exception(PAYROLL_USER_NOT_EXISTS);
            }
            if (!userIds.add(emp.getUserId())) {
                throw exception(PAYROLL_EMPLOYEE_DUPLICATE);
            }
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi != null && CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds)
                : Collections.emptyMap();

        List<CalculatedEmployeeData> result = new ArrayList<>();
        for (PayrollEmployeeSaveReqVO empReq : employees) {
            AdminUserRespDTO user = userMap.get(empReq.getUserId());
            if (user == null) {
                throw exception(PAYROLL_USER_NOT_EXISTS);
            }

            BigDecimal empEarningTotal = BigDecimal.ZERO;
            BigDecimal empDeductionTotal = BigDecimal.ZERO;

            List<CalculatedItemData> calculatedItems = new ArrayList<>();
            boolean hasDefectLine = false;

            if (CollUtil.isNotEmpty(empReq.getItems())) {
                for (PayrollLineItemSaveReqVO itemReq : empReq.getItems()) {
                    String itemKind = itemReq.getItemKind() != null ? itemReq.getItemKind().trim().toUpperCase() : "";
                    String itemType = itemReq.getItemType() != null ? itemReq.getItemType().trim().toUpperCase() : "";

                    BigDecimal amount;
                    if (itemReq.getQty() != null && itemReq.getUnitPrice() != null) {
                        amount = itemReq.getQty().multiply(itemReq.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
                    } else if ("DEFECT".equals(itemType) && CollUtil.isNotEmpty(itemReq.getDefectRefs())
                            && itemReq.getAmount() == null && itemReq.getUnitPrice() == null) {
                        // 统一扣款单价 × 卷数
                        BigDecimal price = itemReq.getDefectRefs().get(0).getDeductPrice();
                        if (price != null) {
                            BigDecimal qty = itemReq.getQty() != null ? itemReq.getQty() : new BigDecimal(itemReq.getDefectRefs().size());
                            amount = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                            if (itemReq.getQty() == null) {
                                itemReq.setQty(qty);
                            }
                            if (itemReq.getUnitPrice() == null) {
                                itemReq.setUnitPrice(price);
                            }
                        } else {
                            amount = null;
                        }
                    } else {
                        amount = itemReq.getAmount() != null
                                ? itemReq.getAmount().setScale(2, RoundingMode.HALF_UP)
                                : null;
                    }

                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                    }

                    if (PayrollItemKindEnum.EARNING.getValue().equals(itemKind)) {
                        if (!PayrollItemKindEnum.VALID_EARNING_TYPES.contains(itemType)) {
                            throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                        }
                        if (CollUtil.isNotEmpty(itemReq.getDefectRefs())) {
                            throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                        }
                        empEarningTotal = empEarningTotal.add(amount);
                    } else if (PayrollItemKindEnum.DEDUCTION.getValue().equals(itemKind)) {
                        if ("DEFECT".equals(itemType)) {
                            // 手工录入行严禁 DEFECT（次品行仅导入生成，07 票）；每员工一条 DEDUCTION/DEFECT 款项行
                            if (hasDefectLine || CollUtil.isEmpty(itemReq.getDefectRefs())) {
                                throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                            }
                            hasDefectLine = true;

                            for (PayrollDefectRefSaveReqVO ref : itemReq.getDefectRefs()) {
                                if (ref.getDeductPrice() == null || ref.getDeductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                    throw exception(PAYROLL_DEFECT_PRICE_REQUIRED);
                                }
                                if (ref.getUserId() == null) {
                                    ref.setUserId(empReq.getUserId());
                                }
                                if (StrUtil.isBlank(ref.getNickname())) {
                                    ref.setNickname(user.getNickname());
                                }
                            }
                            empDeductionTotal = empDeductionTotal.add(amount);
                        } else if (PayrollItemKindEnum.VALID_MANUAL_DEDUCTION_TYPES.contains(itemType)) {
                            if (CollUtil.isNotEmpty(itemReq.getDefectRefs())) {
                                throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                            }
                            empDeductionTotal = empDeductionTotal.add(amount);
                        } else {
                            throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                        }
                    } else {
                        throw exception(PAYROLL_LINE_AMOUNT_INVALID);
                    }

                    PayrollLineItemDO itemDO = new PayrollLineItemDO();
                    itemDO.setItemKind(itemKind);
                    itemDO.setItemType(itemType);
                    itemDO.setQty(itemReq.getQty());
                    itemDO.setUnitPrice(itemReq.getUnitPrice());
                    itemDO.setAmount(amount);
                    itemDO.setRemark(itemReq.getRemark());

                    calculatedItems.add(new CalculatedItemData(itemDO, itemReq.getDefectRefs()));
                }
            }

            empEarningTotal = empEarningTotal.setScale(2, RoundingMode.HALF_UP);
            empDeductionTotal = empDeductionTotal.setScale(2, RoundingMode.HALF_UP);
            BigDecimal empNetAmount = empEarningTotal.subtract(empDeductionTotal).setScale(2, RoundingMode.HALF_UP);

            // 任一员工实发不可为负（025_011）
            if (empNetAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PAYROLL_NET_AMOUNT_NEGATIVE);
            }

            PayrollEmployeeDO empDO = new PayrollEmployeeDO();
            empDO.setUserId(empReq.getUserId());
            empDO.setNickname(user.getNickname());
            empDO.setEarningTotal(empEarningTotal);
            empDO.setDeductionTotal(empDeductionTotal);
            empDO.setNetAmount(empNetAmount);
            empDO.setRemark(empReq.getRemark());

            result.add(new CalculatedEmployeeData(empDO, calculatedItems));
        }

        return result;
    }

    private void recalculatePayrollTotals(PayrollDO payroll, List<CalculatedEmployeeData> calculatedEmployees) {
        BigDecimal earningTotal = BigDecimal.ZERO;
        BigDecimal deductionTotal = BigDecimal.ZERO;
        for (CalculatedEmployeeData item : calculatedEmployees) {
            earningTotal = earningTotal.add(item.getEmployee().getEarningTotal());
            deductionTotal = deductionTotal.add(item.getEmployee().getDeductionTotal());
        }
        earningTotal = earningTotal.setScale(2, RoundingMode.HALF_UP);
        deductionTotal = deductionTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = earningTotal.subtract(deductionTotal).setScale(2, RoundingMode.HALF_UP);

        payroll.setEarningTotal(earningTotal);
        payroll.setDeductionTotal(deductionTotal);
        payroll.setNetAmount(netAmount);
    }

    private void saveEmployeeAndItemRows(Long payrollId, List<CalculatedEmployeeData> calculatedEmployees) {
        for (CalculatedEmployeeData data : calculatedEmployees) {
            PayrollEmployeeDO empDO = data.getEmployee();
            empDO.setPayrollId(payrollId);
            payrollEmployeeMapper.insert(empDO);

            for (CalculatedItemData itemData : data.getItems()) {
                PayrollLineItemDO itemDO = itemData.getItem();
                itemDO.setPayrollId(payrollId);
                itemDO.setEmployeeRowId(empDO.getId());
                payrollLineItemMapper.insert(itemDO);

                // 保存次品引用快照
                if (CollUtil.isNotEmpty(itemData.getDefectRefs())) {
                    for (PayrollDefectRefSaveReqVO refReq : itemData.getDefectRefs()) {
                        PayrollDefectRefDO refDO = new PayrollDefectRefDO();
                        refDO.setLineItemId(itemDO.getId());
                        refDO.setUserId(empDO.getUserId());
                        refDO.setNickname(StrUtil.isNotBlank(refReq.getNickname()) ? refReq.getNickname() : empDO.getNickname());
                        refDO.setConfirmationId(refReq.getConfirmationId());
                        refDO.setConfirmationNo(StrUtil.isNotBlank(refReq.getConfirmationNo())
                                ? refReq.getConfirmationNo()
                                : String.valueOf(refReq.getConfirmationId()));
                        refDO.setRollNo(refReq.getRollNo());
                        refDO.setGrade(refReq.getGrade());
                        refDO.setConfirmedAt(refReq.getConfirmedAt());
                        refDO.setDeductPrice(refReq.getDeductPrice());
                        payrollDefectRefMapper.insert(refDO);
                    }
                }
            }
        }
    }

    private List<PayrollDefectRefSaveReqVO> extractDefectRefs(List<CalculatedEmployeeData> calculatedEmployees) {
        if (CollUtil.isEmpty(calculatedEmployees)) {
            return Collections.emptyList();
        }
        List<PayrollDefectRefSaveReqVO> list = new ArrayList<>();
        for (CalculatedEmployeeData emp : calculatedEmployees) {
            for (CalculatedItemData item : emp.getItems()) {
                if (CollUtil.isNotEmpty(item.getDefectRefs())) {
                    list.addAll(item.getDefectRefs());
                }
            }
        }
        return list;
    }

    private void saveWithGeneratedNo(PayrollDO payroll) {
        String periodPrefix = PAYROLL_NO_PREFIX + payroll.getPeriod().replace("-", "");
        for (int retry = 0; retry < MAX_GENERATE_RETRY; retry++) {
            String candidateNo = generatePayrollNo(periodPrefix);
            payroll.setPayrollNo(candidateNo);
            try {
                payrollMapper.insert(payroll);
                return;
            } catch (DataIntegrityViolationException ex) {
                String message = ex.getMessage() != null ? ex.getMessage() : "";
                // 若违反了同期间唯一索引，抛期间重复错误码 025_002
                if (message.contains("uk_lfp_tenant_period") || message.contains("payroll_period") || message.contains("period")) {
                    throw exception(PAYROLL_PERIOD_DUPLICATE);
                }
                log.warn("[saveWithGeneratedNo] 单号冲突重试: candidateNo={}, retry={}", candidateNo, retry);
            }
        }
        throw exception(PAYROLL_PERIOD_DUPLICATE);
    }

    private synchronized String generatePayrollNo(String periodPrefix) {
        List<PayrollDO> existing = payrollMapper.selectListByPayrollNoPrefixIncludingDeleted(periodPrefix);
        int maxSeq = 0;
        if (CollUtil.isNotEmpty(existing)) {
            for (PayrollDO p : existing) {
                String no = p.getPayrollNo();
                if (StrUtil.isNotBlank(no) && no.startsWith(periodPrefix)) {
                    String suffix = no.substring(periodPrefix.length());
                    if (NO_SUFFIX_NUMBER_PATTERN.matcher(suffix).matches()) {
                        try {
                            int seq = Integer.parseInt(suffix);
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        int nextSeq = maxSeq + 1;
        String nextNo = periodPrefix + String.format("%0" + NO_MIN_DIGITS + "d", nextSeq);
        int offset = 1;
        while (payrollMapper.selectByPayrollNoIncludingDeleted(nextNo) != null && offset <= 100) {
            nextNo = periodPrefix + String.format("%0" + NO_MIN_DIGITS + "d", nextSeq + offset);
            offset++;
        }
        return nextNo;
    }

    private static class CalculatedItemData {
        private final PayrollLineItemDO item;
        private final List<PayrollDefectRefSaveReqVO> defectRefs;

        public CalculatedItemData(PayrollLineItemDO item, List<PayrollDefectRefSaveReqVO> defectRefs) {
            this.item = item;
            this.defectRefs = defectRefs;
        }

        public PayrollLineItemDO getItem() {
            return item;
        }

        public List<PayrollDefectRefSaveReqVO> getDefectRefs() {
            return defectRefs;
        }
    }

    private static class CalculatedEmployeeData {
        private final PayrollEmployeeDO employee;
        private final List<CalculatedItemData> items;

        public CalculatedEmployeeData(PayrollEmployeeDO employee, List<CalculatedItemData> items) {
            this.employee = employee;
            this.items = items;
        }

        public PayrollEmployeeDO getEmployee() {
            return employee;
        }

        public List<CalculatedItemData> getItems() {
            return items;
        }
    }

}

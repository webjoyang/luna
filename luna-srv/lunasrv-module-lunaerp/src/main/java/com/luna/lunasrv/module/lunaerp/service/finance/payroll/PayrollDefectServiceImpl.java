package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectPreviewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectRefSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDefectRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollLineItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollDefectRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollLineItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationResponsibilityEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationStatusEnum;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYROLL_DEFECT_PRICE_REQUIRED;

/**
 * 财务管理 - 工资单次品扣款导入 Service 实现
 *
 * 只读质检确认库（production 域零改动，不回写质检表、不引入 payrollSyncedAt）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PayrollDefectServiceImpl implements PayrollDefectService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    @Resource
    private InspectionConfirmationMapper inspectionConfirmationMapper;

    @Resource
    private PayrollDefectRefMapper payrollDefectRefMapper;

    @Resource
    private PayrollLineItemMapper payrollLineItemMapper;

    @Resource
    private PayrollMapper payrollMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @LogRecord(type = "PAYROLL", subType = "IMPORT_DEFECT", bizNo = "{{#period}}",
            success = "拉取期间【{{#period}}】质检次品扣款预览数据")
    public PayrollDefectPreviewRespVO getDefectPreview(String period) {
        return getDefectPreview(period, null);
    }

    @Override
    @LogRecord(type = "PAYROLL", subType = "IMPORT_DEFECT", bizNo = "{{#period}}",
            success = "拉取期间【{{#period}}】质检次品扣款预览数据")
    public PayrollDefectPreviewRespVO getDefectPreview(String period, Long currentPayrollId) {
        if (!StringUtils.hasText(period) || !PERIOD_PATTERN.matcher(period.trim()).matches()) {
            throw exception(BAD_REQUEST);
        }
        String cleanPeriod = period.trim();

        // 1. 计算月首与次月首（confirmed_at ∈ [period-01, 次月-01)）
        YearMonth ym = YearMonth.parse(cleanPeriod);
        LocalDateTime periodStart = ym.atDay(1).atStartOfDay();
        LocalDateTime nextPeriodStart = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 2. 只读质检确认库：status=CONFIRMED AND responsibility=OPERATOR AND deleted=0 AND confirmed_at ∈ [start, end)
        List<InspectionConfirmationDO> candidates = inspectionConfirmationMapper.selectList(
                new LambdaQueryWrapperX<InspectionConfirmationDO>()
                        .eq(InspectionConfirmationDO::getStatus, InspectionConfirmationStatusEnum.CONFIRMED.getValue())
                        .eq(InspectionConfirmationDO::getResponsibility, InspectionConfirmationResponsibilityEnum.OPERATOR.getValue())
                        .ge(InspectionConfirmationDO::getConfirmedAt, periodStart)
                        .lt(InspectionConfirmationDO::getConfirmedAt, nextPeriodStart)
                        .orderByAsc(InspectionConfirmationDO::getWorkerUserId)
                        .orderByAsc(InspectionConfirmationDO::getConfirmedAt)
                        .orderByAsc(InspectionConfirmationDO::getId));

        if (CollUtil.isEmpty(candidates)) {
            return PayrollDefectPreviewRespVO.builder()
                    .period(cleanPeriod)
                    .groups(Collections.emptyList())
                    .build();
        }

        // 3. 指纹排重：排除已被其它有效工资单（非 CANCELLED、deleted=0）引用的 confirmation_id
        List<Long> candidateIds = candidates.stream()
                .map(InspectionConfirmationDO::getId)
                .collect(Collectors.toList());
        Map<Long, String> occupiedMap = batchGetOccupiedConfirmationMap(candidateIds, currentPayrollId);

        List<InspectionConfirmationDO> availableCandidates = candidates.stream()
                .filter(item -> !occupiedMap.containsKey(item.getId()))
                .filter(item -> item.getWorkerUserId() != null)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(availableCandidates)) {
            return PayrollDefectPreviewRespVO.builder()
                    .period(cleanPeriod)
                    .groups(Collections.emptyList())
                    .build();
        }

        // 4. 按 worker_user_id 聚合卷数+逐卷快照
        Map<Long, List<InspectionConfirmationDO>> grouped = availableCandidates.stream()
                .collect(Collectors.groupingBy(InspectionConfirmationDO::getWorkerUserId, LinkedHashMap::new, Collectors.toList()));

        // 批量查询员工昵称兜底（防 workerName 为空）
        Set<Long> userIds = grouped.keySet();
        Map<Long, AdminUserRespDTO> userMap = adminUserApi != null && CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds)
                : Collections.emptyMap();

        List<PayrollDefectPreviewRespVO.WorkerGroup> groups = new ArrayList<>();
        for (Map.Entry<Long, List<InspectionConfirmationDO>> entry : grouped.entrySet()) {
            Long workerUserId = entry.getKey();
            List<InspectionConfirmationDO> workerRecords = entry.getValue();

            String workerName = workerRecords.stream()
                    .map(InspectionConfirmationDO::getWorkerName)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElseGet(() -> {
                        AdminUserRespDTO u = userMap.get(workerUserId);
                        return u != null ? u.getNickname() : "";
                    });

            List<PayrollDefectPreviewRespVO.DefectRecord> records = workerRecords.stream()
                    .map(item -> {
                        String confirmationNo = StringUtils.hasText(item.getInspectionOrderCode())
                                ? item.getInspectionOrderCode().trim()
                                : String.valueOf(item.getId());
                        return PayrollDefectPreviewRespVO.DefectRecord.builder()
                                .confirmationId(item.getId())
                                .confirmationNo(confirmationNo)
                                .rollNo(item.getRollNo())
                                .vatNo(item.getVatNo())
                                .grade(item.getGrade())
                                .confirmedAt(item.getConfirmedAt())
                                .workerUserId(workerUserId)
                                .workerName(workerName)
                                .build();
                    })
                    .collect(Collectors.toList());

            groups.add(PayrollDefectPreviewRespVO.WorkerGroup.builder()
                    .workerUserId(workerUserId)
                    .workerName(workerName)
                    .count(records.size())
                    .records(records)
                    .build());
        }

        return PayrollDefectPreviewRespVO.builder()
                .period(cleanPeriod)
                .groups(groups)
                .build();
    }

    @Override
    public void validateDefectRefsAvailability(Long currentPayrollId, Collection<Long> confirmationIds) {
        if (CollUtil.isEmpty(confirmationIds)) {
            return;
        }

        // 1. 同单内部重复校验
        Set<Long> uniqueIds = new HashSet<>();
        for (Long id : confirmationIds) {
            if (id == null) {
                continue;
            }
            if (!uniqueIds.add(id)) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, id, "同单重复引用");
            }
        }

        // 2. 跨单占用校验：排除已被有效工资单（非 CANCELLED、deleted=0）引用的 confirmation_id
        Map<Long, String> occupiedMap = batchGetOccupiedConfirmationMap(uniqueIds, currentPayrollId);
        if (CollUtil.isNotEmpty(occupiedMap)) {
            Map.Entry<Long, String> firstOccupied = occupiedMap.entrySet().iterator().next();
            throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, firstOccupied.getKey(), firstOccupied.getValue());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateDefectRefsForSave(Long currentPayrollId, Collection<PayrollDefectRefSaveReqVO> defectRefs) {
        if (CollUtil.isEmpty(defectRefs)) {
            return;
        }

        List<Long> confirmationIds = new ArrayList<>();
        for (PayrollDefectRefSaveReqVO ref : defectRefs) {
            if (ref.getConfirmationId() == null) {
                throw exception(BAD_REQUEST);
            }
            // 单价必填且必须大于0（025_010）
            if (ref.getDeductPrice() == null || ref.getDeductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PAYROLL_DEFECT_PRICE_REQUIRED);
            }
            confirmationIds.add(ref.getConfirmationId());
        }

        // 1. 指纹防重校验
        validateDefectRefsAvailability(currentPayrollId, confirmationIds);

        // 2. 候选质检确认记录重查（防并发被抢、防撤销换链）
        Map<Long, InspectionConfirmationDO> qcMap = batchGetInspectionConfirmationMap(confirmationIds);
        for (PayrollDefectRefSaveReqVO ref : defectRefs) {
            InspectionConfirmationDO qc = qcMap.get(ref.getConfirmationId());
            String displayNo = StrUtil.isNotBlank(ref.getConfirmationNo())
                    ? ref.getConfirmationNo()
                    : String.valueOf(ref.getConfirmationId());

            if (qc == null || Boolean.TRUE.equals(qc.getDeleted())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "质检记录不存在或已被删除");
            }
            if (!InspectionConfirmationStatusEnum.CONFIRMED.getValue().equalsIgnoreCase(qc.getStatus())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "质检记录已撤销或未认定");
            }
            if (!InspectionConfirmationResponsibilityEnum.OPERATOR.getValue().equalsIgnoreCase(qc.getResponsibility())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "质检责任非操作失误");
            }
            if (ref.getUserId() != null && qc.getWorkerUserId() != null && !ref.getUserId().equals(qc.getWorkerUserId())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "责任工人不匹配");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateDefectRefsForSubmit(Long payrollId) {
        if (payrollId == null) {
            return;
        }

        // 1. 查询当前工资单下属所有款项行
        List<PayrollLineItemDO> lineItems = payrollLineItemMapper.selectList(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .eq(PayrollLineItemDO::getPayrollId, payrollId));
        if (CollUtil.isEmpty(lineItems)) {
            return;
        }

        List<Long> lineItemIds = lineItems.stream().map(PayrollLineItemDO::getId).collect(Collectors.toList());
        List<PayrollDefectRefDO> refs = payrollDefectRefMapper.selectList(new LambdaQueryWrapperX<PayrollDefectRefDO>()
                .inOrEmpty(PayrollDefectRefDO::getLineItemId, lineItemIds));
        if (CollUtil.isEmpty(refs)) {
            return;
        }

        List<Long> confirmationIds = refs.stream()
                .map(PayrollDefectRefDO::getConfirmationId)
                .collect(Collectors.toList());

        // 2. 指纹可用性防重
        validateDefectRefsAvailability(payrollId, confirmationIds);

        // 3. 质检确认状态重查（REVOKED 链重验：提交时若已被撤销，报 025_005 要求重新导入）
        Map<Long, InspectionConfirmationDO> qcMap = batchGetInspectionConfirmationMap(confirmationIds);
        for (PayrollDefectRefDO ref : refs) {
            InspectionConfirmationDO qc = qcMap.get(ref.getConfirmationId());
            String displayNo = StrUtil.isNotBlank(ref.getConfirmationNo())
                    ? ref.getConfirmationNo()
                    : String.valueOf(ref.getConfirmationId());

            if (qc == null || Boolean.TRUE.equals(qc.getDeleted())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "质检记录不存在或已被删除");
            }
            if (!InspectionConfirmationStatusEnum.CONFIRMED.getValue().equalsIgnoreCase(qc.getStatus())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "已撤销");
            }
            if (!InspectionConfirmationResponsibilityEnum.OPERATOR.getValue().equalsIgnoreCase(qc.getResponsibility())) {
                throw exception(PAYROLL_DEFECT_ALREADY_REFERENCED, displayNo, "责任类型非操作失误");
            }
        }
    }

    /**
     * 批量查询被有效工资单占用的确认记录（返回 confirmationId -> occupiedPayrollNo 映射）
     */
    private Map<Long, String> batchGetOccupiedConfirmationMap(Collection<Long> confirmationIds, Long currentPayrollId) {
        if (CollUtil.isEmpty(confirmationIds)) {
            return Collections.emptyMap();
        }

        List<PayrollDefectRefDO> refs = payrollDefectRefMapper.selectList(new LambdaQueryWrapperX<PayrollDefectRefDO>()
                .inOrEmpty(PayrollDefectRefDO::getConfirmationId, confirmationIds));
        if (CollUtil.isEmpty(refs)) {
            return Collections.emptyMap();
        }

        Set<Long> lineItemIds = refs.stream()
                .map(PayrollDefectRefDO::getLineItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> lineItemToPayrollMap = batchGetLineItemToPayrollMap(lineItemIds);

        Set<Long> payrollIds = new HashSet<>(lineItemToPayrollMap.values());
        Map<Long, PayrollDO> payrollMap = batchGetPayrollMap(payrollIds);

        Map<Long, String> occupiedMap = new HashMap<>();
        for (PayrollDefectRefDO ref : refs) {
            Long payrollId = lineItemToPayrollMap.get(ref.getLineItemId());
            if (payrollId == null) {
                continue;
            }
            // 排除自身单据
            if (currentPayrollId != null && currentPayrollId.equals(payrollId)) {
                continue;
            }
            PayrollDO payroll = payrollMap.get(payrollId);
            if (payroll == null) {
                continue;
            }
            // 释放语义：CANCELLED 工资单释放指纹
            if (ApplyStatus.CANCELLED.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
                continue;
            }
            // 释放语义：软删草稿工资单释放指纹
            if (Boolean.TRUE.equals(payroll.getDeleted())) {
                continue;
            }

            occupiedMap.put(ref.getConfirmationId(), payroll.getPayrollNo());
        }

        return occupiedMap;
    }

    private Map<Long, Long> batchGetLineItemToPayrollMap(Collection<Long> lineItemIds) {
        if (CollUtil.isEmpty(lineItemIds)) {
            return Collections.emptyMap();
        }
        List<PayrollLineItemDO> lineItems = payrollLineItemMapper.selectList(new LambdaQueryWrapperX<PayrollLineItemDO>()
                .inOrEmpty(PayrollLineItemDO::getId, lineItemIds));
        Map<Long, Long> map = new HashMap<>();
        for (PayrollLineItemDO item : lineItems) {
            map.put(item.getId(), item.getPayrollId());
        }
        return map;
    }

    private Map<Long, PayrollDO> batchGetPayrollMap(Collection<Long> payrollIds) {
        if (CollUtil.isEmpty(payrollIds)) {
            return Collections.emptyMap();
        }
        List<PayrollDO> payrolls = payrollMapper.selectList(new LambdaQueryWrapperX<PayrollDO>()
                .inOrEmpty(PayrollDO::getId, payrollIds));
        Map<Long, PayrollDO> map = new HashMap<>();
        for (PayrollDO p : payrolls) {
            map.put(p.getId(), p);
        }
        return map;
    }

    private Map<Long, InspectionConfirmationDO> batchGetInspectionConfirmationMap(Collection<Long> confirmationIds) {
        if (CollUtil.isEmpty(confirmationIds)) {
            return Collections.emptyMap();
        }
        List<InspectionConfirmationDO> confirmations = inspectionConfirmationMapper.selectList(
                new LambdaQueryWrapperX<InspectionConfirmationDO>()
                        .inOrEmpty(InspectionConfirmationDO::getId, confirmationIds));
        Map<Long, InspectionConfirmationDO> map = new HashMap<>();
        for (InspectionConfirmationDO c : confirmations) {
            map.put(c.getId(), c);
        }
        return map;
    }

}

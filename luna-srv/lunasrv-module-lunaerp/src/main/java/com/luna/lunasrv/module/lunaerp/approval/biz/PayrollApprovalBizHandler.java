package com.luna.lunasrv.module.lunaerp.approval.biz;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollEmployeeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollEmployeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 员工工资单审批业务处理器
 *
 * 供 02 号审批引擎发起审批实例时校验单据、提取单号快照与同步 approval_status。
 *
 * @author luna
 */
@Component
public class PayrollApprovalBizHandler implements ApprovalBizHandler {

    @Resource
    private PayrollMapper payrollMapper;

    @Resource
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @Override
    public String getBizType() {
        return ApprovalHandler.BIZ_TYPE_PAYROLL;
    }

    @Override
    public String getBizTypeName() {
        return "员工工资单";
    }

    @Override
    public String getBizNo(Long bizId) {
        PayrollDO payroll = payrollMapper.selectById(bizId);
        return payroll != null ? payroll.getPayrollNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        PayrollDO payroll = payrollMapper.selectById(bizId);
        if (payroll == null) {
            throw exception(PAYROLL_NOT_EXISTS);
        }
        if (ApplyStatus.APPROVING.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "工资单审批中，不可重复提交");
        }
        if (!ApplyStatus.isDraft(payroll.getApprovalStatus())
                && !ApplyStatus.REJECTED.getValue().equalsIgnoreCase(payroll.getApprovalStatus())) {
            throw exception(PAYROLL_STATUS_NOT_ALLOWED);
        }

        // 校验至少1人
        List<PayrollEmployeeDO> employees = payrollEmployeeMapper.selectList(new LambdaQueryWrapperX<PayrollEmployeeDO>()
                .eq(PayrollEmployeeDO::getPayrollId, bizId));
        if (CollUtil.isEmpty(employees)) {
            throw exception(PAYROLL_EMPTY);
        }

        // 校验全员实发非负
        for (PayrollEmployeeDO emp : employees) {
            if (emp.getNetAmount() == null || emp.getNetAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PAYROLL_NET_AMOUNT_NEGATIVE);
            }
        }

        // 校验期间唯一
        Long duplicateCount = payrollMapper.selectCount(new LambdaQueryWrapperX<PayrollDO>()
                .eq(PayrollDO::getPeriod, payroll.getPeriod())
                .ne(PayrollDO::getApprovalStatus, "CANCELLED")
                .ne(PayrollDO::getId, bizId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw exception(PAYROLL_PERIOD_DUPLICATE);
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        String status = approvalStatus;
        if (ApplyStatus.isDraft(approvalStatus) || "CANCELLED".equalsIgnoreCase(approvalStatus)) {
            status = ApplyStatus.PENDING.getValue();
        }

        LambdaUpdateWrapper<PayrollDO> updateWrapper = new LambdaUpdateWrapper<PayrollDO>()
                .eq(PayrollDO::getId, bizId)
                .set(PayrollDO::getApprovalStatus, status);

        if (ApplyStatus.APPROVED.getValue().equalsIgnoreCase(status)) {
            updateWrapper.set(PayrollDO::getApproveTime, LocalDateTime.now());
        }

        payrollMapper.update(null, updateWrapper);
    }

}

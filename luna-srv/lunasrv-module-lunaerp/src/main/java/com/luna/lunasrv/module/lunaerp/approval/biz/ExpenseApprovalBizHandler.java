package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.EXPENSE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED;

/**
 * 费用报销审批业务处理器
 *
 * 供 02 号审批引擎发起审批实例时校验单据、提取单号快照与同步 approval_status。
 *
 * @author luna
 */
@Component
public class ExpenseApprovalBizHandler implements ApprovalBizHandler {

    @Resource
    private ExpenseMapper expenseMapper;

    @Override
    public String getBizType() {
        return ApprovalHandler.BIZ_TYPE_EXPENSE;
    }

    @Override
    public String getBizTypeName() {
        return "费用报销";
    }

    @Override
    public String getBizNo(Long bizId) {
        ExpenseDO expense = expenseMapper.selectById(bizId);
        return expense != null ? expense.getExpenseNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        ExpenseDO expense = expenseMapper.selectById(bizId);
        if (expense == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        if (ApplyStatus.APPROVING.getValue().equalsIgnoreCase(expense.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "报销单审批中，不可重复提交");
        }
        if (!ApplyStatus.isDraft(expense.getApprovalStatus())
                && !ApplyStatus.REJECTED.getValue().equalsIgnoreCase(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        String status = approvalStatus;
        if (ApplyStatus.isDraft(approvalStatus) || "CANCELLED".equalsIgnoreCase(approvalStatus)) {
            status = ApplyStatus.PENDING.getValue();
        }
        expenseMapper.update(null, new LambdaUpdateWrapper<ExpenseDO>()
                .eq(ExpenseDO::getId, bizId)
                .set(ExpenseDO::getApprovalStatus, status));
    }

}

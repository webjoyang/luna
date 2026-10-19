package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.luna.lunasrv.module.lunaerp.service.finance.expense.ExpenseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 员工费用报销审批通过副作用处理器（对齐 ADR-0017 / ADR-0043 与 Issue 05）
 *
 * 关键语义：一步制过账。在 BEFORE_COMMIT 事务内通过统一过账通道发起 CREDIT/OUT_EXPENSE 扣款。
 * 余额不足抛出异常，整个审批事务回滚。
 *
 * @author luna
 */
@Component
@Slf4j
public class ExpenseApprovalHandler implements ApprovalHandler {

    @Resource
    private ExpenseService expenseService;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_EXPENSE.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[ExpenseApprovalHandler] 员工费用报销审批通过，同事务统一通道过账: bizId={}", bizId);
        expenseService.approveExpenseAndPost(bizId);
    }

}

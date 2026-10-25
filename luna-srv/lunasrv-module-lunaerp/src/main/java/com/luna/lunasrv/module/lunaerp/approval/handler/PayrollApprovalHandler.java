package com.luna.lunasrv.module.lunaerp.approval.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 员工工资单审批通过副作用处理器（对齐 ADR-0017 / ADR-0043 与 Issue 06）
 *
 * 关键语义：两步制发放。审批通过 Handler 为空实现，引擎仅置 APPROVED 并记录通过时间；
 * 不直接写资金账流水，实际过账动作由出纳后续登记发放（对齐 11 号付款两步制先例）。
 *
 * @author luna
 */
@Component
@Slf4j
public class PayrollApprovalHandler implements ApprovalHandler {

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_PAYROLL.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        // 空实现：两步制发放，审批通过不触发资金过账，写账动作在后续发放登记（08 票）
        log.info("[PayrollApprovalHandler] 工资单审批通过（两步制空实现，不写资金账，由出纳后续登记发放）: bizId={}", bizId);
    }

}

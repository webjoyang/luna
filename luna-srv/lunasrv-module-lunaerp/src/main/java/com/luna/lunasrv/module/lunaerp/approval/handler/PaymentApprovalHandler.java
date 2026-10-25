package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 付款申请审批通过副作用处理器（对齐 Issue 05 与 ADR-0017）
 *
 * 关键语义：审批通过不发生任何资金动作，Handler 只回写状态（零账务写入），钱要等下一张票的付款登记。
 *
 * @author luna
 */
@Component
@Slf4j
public class PaymentApprovalHandler implements ApprovalHandler {

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_PAYMENT.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[PaymentApprovalHandler] 付款申请审批通过，仅置 approval_status 为 APPROVED，零账务写入: bizId={}", bizId);
        int rows = paymentApplyMapper.update(null, new LambdaUpdateWrapper<PaymentApplyDO>()
                .eq(PaymentApplyDO::getId, bizId)
                .set(PaymentApplyDO::getApprovalStatus, ApplyStatus.APPROVED.getValue()));
        if (rows == 0) {
            log.warn("[PaymentApprovalHandler] 未找到付款申请或更新行数为0: bizId={}", bizId);
        }
    }

}

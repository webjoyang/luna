package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 付款申请审批通过副作用处理器（对齐 ADR-0017 与 Spec）
 *
 * 监听 payment 审批通过，仅将付款申请主表 approval_status 置为 APPROVED。
 * 核心语义：审批通过不发生任何资金动作，零账务/资金写入（付款登记才是资金动作）。
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
        log.info("[PaymentApprovalHandler] 付款申请审批通过, 更新 approval_status=APPROVED, 零账务写入: bizId={}", bizId);
        int rows = paymentApplyMapper.update(null, new LambdaUpdateWrapper<PaymentApplyDO>()
                .eq(PaymentApplyDO::getId, bizId)
                .set(PaymentApplyDO::getApprovalStatus, "APPROVED"));
        if (rows == 0) {
            log.warn("[PaymentApprovalHandler] 未找到付款申请或更新行数为0: bizId={}", bizId);
        }
    }

}

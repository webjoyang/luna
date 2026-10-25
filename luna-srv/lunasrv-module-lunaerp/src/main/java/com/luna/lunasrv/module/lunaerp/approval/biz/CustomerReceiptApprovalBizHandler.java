package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.RECEIPT_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW;

/**
 * 客户收款审批业务处理器
 *
 * 供 02 号审批引擎发起审批实例时校验单据、提取单号快照与同步 approval_status。
 *
 * @author luna
 */
@Component
public class CustomerReceiptApprovalBizHandler implements ApprovalBizHandler {

    @Resource
    private PaymentMapper paymentMapper;

    @Override
    public String getBizType() {
        return ApprovalHandler.BIZ_TYPE_CUSTOMER_RECEIPT;
    }

    @Override
    public String getBizTypeName() {
        return "客户收款";
    }

    @Override
    public String getBizNo(Long bizId) {
        PaymentDO payment = paymentMapper.selectById(bizId);
        return payment != null ? payment.getPaymentNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        PaymentDO payment = paymentMapper.selectById(bizId);
        if (payment == null || !"IN".equalsIgnoreCase(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        String approvalStatus = payment.getApprovalStatus();
        if (!ApplyStatus.isDraft(approvalStatus)) {
            throw exception(RECEIPT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        paymentMapper.update(null, new LambdaUpdateWrapper<PaymentDO>()
                .eq(PaymentDO::getId, bizId)
                .set(PaymentDO::getApprovalStatus, approvalStatus));
    }

}

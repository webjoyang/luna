package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.RECEIPT_NOT_EXISTS;

/**
 * 客户收款审批业务处理器
 *
 * @author luna
 */
@Component
public class CustomerReceiptApprovalBizHandler implements ApprovalBizHandler {

    public static final String BIZ_TYPE = "CUSTOMER_RECEIPT";

    @Resource
    private PaymentMapper paymentMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
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
        if (payment == null) {
            throw exception(RECEIPT_NOT_EXISTS);
        }
        if ("APPROVING".equalsIgnoreCase(payment.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "客户收款审批中，不可重复提交");
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        paymentMapper.update(null, new LambdaUpdateWrapper<PaymentDO>()
                .eq(PaymentDO::getId, bizId)
                .set(PaymentDO::getApprovalStatus, approvalStatus));
    }

}

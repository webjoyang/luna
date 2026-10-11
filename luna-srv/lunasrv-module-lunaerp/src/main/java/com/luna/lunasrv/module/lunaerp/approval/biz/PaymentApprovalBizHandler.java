package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS;

/**
 * 付款申请审批业务处理器
 *
 * @author luna
 */
@Component
public class PaymentApprovalBizHandler implements ApprovalBizHandler {

    public static final String BIZ_TYPE = "payment";

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    public String getBizTypeName() {
        return "付款申请";
    }

    @Override
    public String getBizNo(Long bizId) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(bizId);
        return apply != null ? apply.getApplyNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        PaymentApplyDO apply = paymentApplyMapper.selectById(bizId);
        if (apply == null) {
            throw exception(PAYMENT_APPLY_NOT_EXISTS);
        }
        if ("APPROVING".equalsIgnoreCase(apply.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "付款申请审批中，不可重复提交");
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        paymentApplyMapper.update(null, new LambdaUpdateWrapper<PaymentApplyDO>()
                .eq(PaymentApplyDO::getId, bizId)
                .set(PaymentApplyDO::getApprovalStatus, approvalStatus));
    }

}

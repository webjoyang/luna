package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_APPLY_STATUS_NOT_ALLOWED;

/**
 * 付款申请审批业务处理器
 *
 * 供 02 号审批引擎创建实例时获取单号、校验单据状态并更新审批态。
 *
 * @author luna
 */
@Component
public class PaymentApplyApprovalBizHandler implements ApprovalBizHandler {

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Override
    public String getBizType() {
        return ApprovalHandler.BIZ_TYPE_PAYMENT;
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
        if (ApplyStatus.APPROVING.getValue().equalsIgnoreCase(apply.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "付款申请审批中，不可重复提交");
        }
        if (!ApplyStatus.isDraft(apply.getApprovalStatus())
                && !ApplyStatus.REJECTED.getValue().equalsIgnoreCase(apply.getApprovalStatus())) {
            throw exception(PAYMENT_APPLY_STATUS_NOT_ALLOWED);
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        paymentApplyMapper.update(null, new LambdaUpdateWrapper<PaymentApplyDO>()
                .eq(PaymentApplyDO::getId, bizId)
                .set(PaymentApplyDO::getApprovalStatus, approvalStatus));
    }

}

package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户收款审批通过副作用处理器（对齐 ADR-0017 与 Issue 04）
 *
 * 监听 ApprovalCompletedEvent（BEFORE_COMMIT），同事务执行资金账户入账、分录记录与核销落库。
 *
 * @author luna
 */
@Component
@Slf4j
public class CustomerReceiptApprovalHandler implements ApprovalHandler {

    public static final String BIZ_TYPE = "CUSTOMER_RECEIPT";

    @Resource
    private CustomerReceiptService customerReceiptService;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[CustomerReceiptApprovalHandler] 执行客户收款审批通过同事务过账: bizId={}", bizId);
        customerReceiptService.postReceiptOnApproval(bizId);
    }

}

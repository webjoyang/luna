package com.luna.lunasrv.module.lunaerp.approval.handler;

/**
 * 审批通过副作用处理器策略接口（对齐 ADR-0017 与 Issue 07）
 *
 * 各业务模块实现此接口并注册为 Spring Bean，由 {@link com.luna.lunasrv.module.lunaerp.approval.listener.ApprovalCompletedEventListener}
 * 在 {@code @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)} 中按 bizType 路由调度。
 * 审批引擎自身不直接写业务账目，副作用由各业务 handler 自行处理。
 * 若业务侧过账/生效抛出异常，事务回滚，审批不生效——保证「审批通过 = 业务生效」强原子性。
 *
 * @author luna
 */
public interface ApprovalHandler {

    /**
     * 单据类型预留点常量
     */
    String BIZ_TYPE_SALES_CONTRACT = "sales_contract";
    String BIZ_TYPE_PAYMENT = "payment";
    String BIZ_TYPE_EXPENSE = "expense";
    String BIZ_TYPE_PAYROLL = "payroll";
    String BIZ_TYPE_SALES_RETURN = "sales_return";
    String BIZ_TYPE_CUSTOMER_RECEIPT = "CUSTOMER_RECEIPT";

    /**
     * 是否支持该业务类型
     *
     * @param bizType 单据类型（如 sales_contract 等）
     * @return 是否支持
     */
    boolean supports(String bizType);

    /**
     * 审批通过后的业务生效/过账副作用处理
     *
     * @param bizId 业务单据ID
     */
    void onApproved(Long bizId);

}

package com.luna.lunasrv.module.lunaerp.approval.biz;

/**
 * 业务单据审批处理器策略接口
 *
 * 各强制审批单据（销售合同/付款申请/报销/工资单/销售退货）实现本接口，
 * 审批引擎统一通过此接口校验单据、获取单号快照以及更新 approval_status。
 *
 * @author luna
 */
public interface ApprovalBizHandler {

    /**
     * 单据类型（如 sales_contract 等）
     */
    String getBizType();

    /**
     * 单据类型中文名称（用于通知与日志，如 "销售合同"）
     */
    String getBizTypeName();

    /**
     * 获取业务单号（快照与通知使用）
     */
    String getBizNo(Long bizId);

    /**
     * 校验业务单据存在且当前状态允许发起审批
     *
     * @param bizId 业务单据ID
     */
    void validateCanSubmit(Long bizId);

    /**
     * 更新业务单据的审批状态
     *
     * @param bizId 业务单据ID
     * @param approvalStatus 状态（DRAFT / APPROVING / APPROVED / REJECTED / CANCELLED）
     */
    void updateApprovalStatus(Long bizId, String approvalStatus);

}

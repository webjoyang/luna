package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import jakarta.validation.Valid;

/**
 * 审批实例 Service 接口
 *
 * @author luna
 */
public interface ApprovalInstanceService {

    /**
     * 发起审批
     *
     * @param submitReqVO 提交信息
     * @return 审批实例ID
     */
    Long submitInstance(@Valid ApprovalInstanceSubmitReqVO submitReqVO);

    /**
     * 撤回审批（仅发起人本人，仅 APPROVING 状态）
     *
     * @param id 审批实例ID
     * @param withdrawReqVO 撤回信息（可选原因）
     */
    void withdrawInstance(Long id, ApprovalInstanceWithdrawReqVO withdrawReqVO);

    /**
     * 获取审批实例详情
     *
     * @param id 审批实例ID
     * @return 实例详情（含任务与操作记录）
     */
    ApprovalInstanceRespVO getInstance(Long id);

    /**
     * 根据业务单据获取最新的审批实例
     *
     * @param bizType 业务单据类型
     * @param bizId 业务单据ID
     * @return 实例详情（含任务与操作记录）
     */
    ApprovalInstanceRespVO getLatestInstanceByBiz(String bizType, Long bizId);

}

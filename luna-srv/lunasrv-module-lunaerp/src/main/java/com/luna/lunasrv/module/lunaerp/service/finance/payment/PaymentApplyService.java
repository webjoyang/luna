package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplySaveReqVO;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * 财务管理 - 付款申请服务接口
 *
 * @author luna
 */
public interface PaymentApplyService {

    /**
     * 获得付款申请分页
     *
     * @param pageReqVO 分页查询条件
     * @return 付款申请分页
     */
    PageResult<PaymentApplyRespVO> getPaymentApplyPage(@Valid PaymentApplyPageReqVO pageReqVO);

    /**
     * 获得付款申请详情
     *
     * @param id 付款申请ID
     * @return 付款申请详情（包含已核销金额 allocatedAmount 聚合）
     */
    PaymentApplyRespVO getPaymentApply(Long id);

    /**
     * 创建付款申请
     *
     * @param createReqVO 保存参数
     * @return 付款申请ID
     */
    Long createPaymentApply(@Valid PaymentApplySaveReqVO createReqVO);

    /**
     * 更新付款申请（仅 DRAFT / PENDING / REJECTED 状态）
     *
     * @param updateReqVO 更新参数
     */
    void updatePaymentApply(@Valid PaymentApplySaveReqVO updateReqVO);

    /**
     * 删除付款申请（仅 DRAFT / PENDING 状态，软删除进回收站）
     *
     * @param id 付款申请ID
     */
    void deletePaymentApply(Long id);

    /**
     * 提交付款申请审批（提交到 02 号审批引擎，状态变更为 APPROVING）
     *
     * @param id 付款申请ID
     * @return 审批实例ID
     */
    Long submitPaymentApply(Long id);

    /**
     * 撤回付款申请审批（仅 APPROVING 状态，状态撤回为 PENDING）
     *
     * @param id 付款申请ID
     */
    void cancelApprovePaymentApply(Long id);

    /**
     * 查询指定供应商的实时预付余额
     * 口径：Σ(PREPAY 类且被核销的 payment_ref.allocate_amount) - Σ(所有 payment.prepay_used_amount WHERE supplierId = ? AND direction = 'OUT' AND status = 'COMPLETED')
     *
     * @param supplierId 供应商ID
     * @return 预付余额（保留两位小数）
     */
    BigDecimal getPrepayBalance(Long supplierId);

}

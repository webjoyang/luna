package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyUpdateReqVO;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * 财务管理 - 付款申请 Service 接口
 *
 * @author luna
 */
public interface PaymentApplyService {

    /**
     * 创建付款申请（RECON 对账付款 / PREPAY 预付付款）
     *
     * @param createReqVO 创建信息
     * @return 申请单ID
     */
    Long createPaymentApply(@Valid PaymentApplyCreateReqVO createReqVO);

    /**
     * 修改付款申请（仅 DRAFT/PENDING/REJECTED 状态）
     *
     * @param updateReqVO 修改信息
     */
    void updatePaymentApply(@Valid PaymentApplyUpdateReqVO updateReqVO);

    /**
     * 删除付款申请（仅 DRAFT/PENDING 状态软删进入回收站）
     *
     * @param id 申请单ID
     */
    void deletePaymentApply(Long id);

    /**
     * 获得付款申请详情（含已核销金额聚合）
     *
     * @param id 申请单ID
     * @return 付款申请详情
     */
    PaymentApplyRespVO getPaymentApply(Long id);

    /**
     * 获得付款申请分页
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<PaymentApplyRespVO> getPaymentApplyPage(@Valid PaymentApplyPageReqVO pageReqVO);

    /**
     * 提交付款申请审批（走 02 号审批引擎）
     *
     * @param id 申请单ID
     */
    void submitPaymentApply(Long id);

    /**
     * 撤回付款申请审批（仅 APPROVING 状态可撤回回 DRAFT/PENDING）
     *
     * @param id 申请单ID
     */
    void cancelApprove(Long id);

    /**
     * 获得供应商实时预付余额
     * <p>口径：Σ(PREPAY 类申请被核销的 allocate_amount) - Σ(所有付款登记 prepay_used_amount)</p>
     *
     * @param supplierId 供应商ID
     * @return 预付余额（元）
     */
    BigDecimal getPrepayBalance(Long supplierId);

}

package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;

import java.util.List;

/**
 * 财务管理 - 客户收款与核销服务接口（IN 侧）
 *
 * @author luna
 */
public interface CustomerReceiptService {

    /**
     * 创建客户收款单草稿（PENDING）
     *
     * @param reqVO 保存请求
     * @return 收款单ID
     */
    Long createReceipt(ReceiptSaveReqVO reqVO);

    /**
     * 更新客户收款单草稿（仅允许 PENDING 状态）
     *
     * @param reqVO 保存请求
     */
    void updateReceipt(ReceiptSaveReqVO reqVO);

    /**
     * 删除客户收款单草稿（仅允许 PENDING 状态）
     *
     * @param id 收款单ID
     */
    void deleteReceipt(Long id);

    /**
     * 提交客户收款单至审批引擎（PENDING/REJECTED -> APPROVING）
     *
     * @param id 收款单ID
     * @return 审批实例ID
     */
    Long submitReceipt(Long id);

    /**
     * 自动核销分配试算（按发货日 FIFO 建议分配，不落库）
     *
     * @param reqVO 试算请求
     * @return 试算结果
     */
    AllocatePreviewRespVO allocatePreview(AllocatePreviewReqVO reqVO);

    /**
     * 查询客户指定币种的未核销应收分录列表（三视角选单数据源，含货款分录与运费分录）
     *
     * @param customerId 客户ID
     * @param currency 币种
     * @return 未核销分录列表
     */
    List<ArBalanceRespVO> getArBalanceList(Long customerId, String currency);

    /**
     * 获取客户收款单详情
     *
     * @param id 收款单ID
     * @return 收款单详情
     */
    PaymentRespVO getReceipt(Long id);

    /**
     * 分页查询客户收款单
     *
     * @param pageReqVO 查询条件
     * @return 分页结果
     */
    PageResult<PaymentRespVO> getReceiptPage(PaymentPageReqVO pageReqVO);

    /**
     * 审批通过后同事务过账（由 CustomerReceiptApprovalHandler 触发）
     * 同事务四写：
     * 1. 资金账户余额条件更新 current_balance += payAmount
     * 2. 经营分录入账（RECEIPT/ADVANCE）
     * 3. 核销行落库（ref_type=AR_ENTRY，ref_id=发货明细/运费分录ID）
     * 4. 收款单状态变更为 COMPLETED，审批状态 APPROVED
     *
     * @param paymentId 收款单ID
     */
    void postReceiptOnApproval(Long paymentId);

}

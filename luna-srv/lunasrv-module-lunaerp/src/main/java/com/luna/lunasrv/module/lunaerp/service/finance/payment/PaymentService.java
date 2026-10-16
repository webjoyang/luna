package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRespVO;
import jakarta.validation.Valid;

/**
 * 财务管理 - 收付款（OUT 侧付款登记）Service 接口
 *
 * @author luna
 */
public interface PaymentService {

    /**
     * 付款登记（出纳执行·OUT 侧）
     *
     * @param reqVO 登记入参
     * @return 付款单ID
     */
    Long registerPayment(@Valid PaymentRegisterReqVO reqVO);

    /**
     * 获得付款单详情（内嵌 ref 核销行）
     *
     * @param id 付款单ID
     * @return 付款单详情
     */
    PaymentRespVO getPayment(Long id);

    /**
     * 获得付款单分页列表（direction=OUT 固定）
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<PaymentRespVO> getPaymentPage(@Valid PaymentPageReqVO pageReqVO);

    /**
     * 创建收款单草稿（仅 approval_status=PENDING）
     *
     * @param createReqVO 保存入参
     * @return 收款单ID
     */
    Long createReceipt(@Valid com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO createReqVO);

    /**
     * 修改收款单草稿（仅 approval_status=PENDING）
     *
     * @param updateReqVO 修改入参
     */
    void updateReceipt(@Valid com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptUpdateReqVO updateReqVO);

    /**
     * 删除收款单草稿（仅 approval_status=PENDING）
     *
     * @param id 收款单ID
     */
    void deleteReceipt(Long id);

    /**
     * 获得收款单详情（内嵌核销分配行）
     *
     * @param id 收款单ID
     * @return 收款单详情
     */
    com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO getReceipt(Long id);

    /**
     * 获得收款单分页列表（direction=IN 固定）
     *
     * @param pageReqVO 分页查询参数
     * @return 分页结果
     */
    PageResult<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO> getReceiptPage(
            @Valid com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptPageReqVO pageReqVO);

    /**
     * 提交收款单审批（仅 PENDING/REJECTED 可提，进入 02 号审批引擎 biz_type=CUSTOMER_RECEIPT）
     *
     * @param id 收款单ID
     */
    void submitReceipt(Long id);

    /**
     * 收款单自动分配试算（FIFO 发货日升序分配，纯内存试算不落库）
     *
     * @param reqVO 试算参数
     * @return 试算结果行列表
     */
    java.util.List<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewRespVO> allocatePreview(
            @Valid com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewReqVO reqVO);

    /**
     * 获得客户指定币种的未核销明细分录列表（三视角选单底层数据源）
     *
     * @param customerId 客户ID
     * @param currency 币种
     * @return 未核销分录列表
     */
    java.util.List<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ArBalanceRespVO> getArBalanceList(
            Long customerId, String currency);

    /**
     * 收款单红冲（仅 status=COMPLETED 可冲，同事务生成 RECEIPT_REVERSAL 分录 + 资金账户扣减 + 核销行重开 + 置 REVERSED）
     *
     * @param id 收款单ID
     */
    void reverseReceipt(Long id);

    /**
     * 预收转核销（在原已过账 ADVANCE 单上追加核销行，不建新单，不走审批）
     *
     * @param reqVO 预收转核销入参
     */
    void advanceWriteoff(@Valid com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO reqVO);

}

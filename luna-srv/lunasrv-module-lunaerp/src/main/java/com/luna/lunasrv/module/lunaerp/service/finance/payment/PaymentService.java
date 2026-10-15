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

}

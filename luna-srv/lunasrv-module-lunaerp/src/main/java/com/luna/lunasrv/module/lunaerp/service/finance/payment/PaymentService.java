package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRespVO;
import jakarta.validation.Valid;

/**
 * 财务管理 - 付款登记服务接口
 *
 * @author luna
 */
public interface PaymentService {

    /**
     * 付款登记（出纳执行·OUT 侧：多申请核销 + 预付抵扣 + 资金扣款 + 经营分录）
     *
     * @param reqVO 登记参数
     * @return 付款单ID
     */
    Long registerPayment(@Valid PaymentRegisterReqVO reqVO);

    /**
     * 获得付款单分页（固定 direction = 'OUT'）
     *
     * @param reqVO 分页查询条件
     * @return 付款单分页
     */
    PageResult<PaymentRespVO> getPaymentPage(@Valid PaymentPageReqVO reqVO);

    /**
     * 获得付款单详情（包含核销明细列表 items）
     *
     * @param id 付款单ID
     * @return 付款单详情
     */
    PaymentRespVO getPayment(Long id);

}

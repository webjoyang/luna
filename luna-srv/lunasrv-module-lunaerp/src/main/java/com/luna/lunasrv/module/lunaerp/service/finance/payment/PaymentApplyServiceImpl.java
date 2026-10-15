package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 财务管理 - 付款申请 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PaymentApplyServiceImpl implements PaymentApplyService {

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

}

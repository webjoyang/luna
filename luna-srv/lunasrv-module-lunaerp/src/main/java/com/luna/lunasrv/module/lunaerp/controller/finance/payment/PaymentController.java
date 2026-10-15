package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务管理 - 收付款（OUT 侧付款登记）控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 收付款")
@RestController
@RequestMapping("/lunaerp/finance/payment")
@Validated
public class PaymentController {

    @Resource
    private PaymentService paymentService;

}

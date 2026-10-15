package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 收付款（OUT 侧付款登记）控制器
 *
 * <p>注：付款单为写账凭证，不提供 update/delete 端点，红冲走二期。</p>
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

    @PostMapping("/register")
    @Operation(summary = "付款登记（出纳执行·OUT 侧：多申请核销 + 预付抵扣 + 账户扣款 + 分录）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Long> registerPayment(@Valid @RequestBody PaymentRegisterReqVO reqVO) {
        return success(paymentService.registerPayment(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款单详情（内嵌 ref 核销行）")
    @Parameter(name = "id", description = "付款单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PaymentRespVO> getPayment(@RequestParam("id") Long id) {
        return success(paymentService.getPayment(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得付款单分页列表（direction=OUT 固定）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<PaymentRespVO>> getPaymentPage(@Valid PaymentPageReqVO pageReqVO) {
        return success(paymentService.getPaymentPage(pageReqVO));
    }

}

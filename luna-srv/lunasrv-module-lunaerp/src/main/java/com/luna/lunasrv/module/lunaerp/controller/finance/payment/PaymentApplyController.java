package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplySaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 付款申请 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 付款申请")
@RestController
@RequestMapping("/lunaerp/finance/payment-apply")
@Validated
public class PaymentApplyController {

    @Resource
    private PaymentApplyService paymentApplyService;

    @GetMapping("/page")
    @Operation(summary = "获得付款申请分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<PaymentApplyRespVO>> getPaymentApplyPage(@Valid PaymentApplyPageReqVO pageReqVO) {
        return success(paymentApplyService.getPaymentApplyPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款申请详情")
    @Parameter(name = "id", description = "付款申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PaymentApplyRespVO> getPaymentApply(@RequestParam("id") Long id) {
        return success(paymentApplyService.getPaymentApply(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建付款申请")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Long> createPaymentApply(@Valid @RequestBody PaymentApplySaveReqVO createReqVO) {
        return success(paymentApplyService.createPaymentApply(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新付款申请")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> updatePaymentApply(@Valid @RequestBody PaymentApplySaveReqVO updateReqVO) {
        paymentApplyService.updatePaymentApply(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除付款申请")
    @Parameter(name = "id", description = "付款申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> deletePaymentApply(@RequestParam("id") Long id) {
        paymentApplyService.deletePaymentApply(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交付款申请审批")
    @Parameter(name = "id", description = "付款申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Long> submitPaymentApply(@RequestParam("id") Long id) {
        return success(paymentApplyService.submitPaymentApply(id));
    }

    @PostMapping("/cancel-approve")
    @Operation(summary = "撤回付款申请审批")
    @Parameter(name = "id", description = "付款申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> cancelApprovePaymentApply(@RequestParam("id") Long id) {
        paymentApplyService.cancelApprovePaymentApply(id);
        return success(true);
    }

    @GetMapping("/prepay-balance")
    @Operation(summary = "获得供应商实时预付余额")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<BigDecimal> getPrepayBalance(@RequestParam("supplierId") Long supplierId) {
        return success(paymentApplyService.getPrepayBalance(supplierId));
    }

}

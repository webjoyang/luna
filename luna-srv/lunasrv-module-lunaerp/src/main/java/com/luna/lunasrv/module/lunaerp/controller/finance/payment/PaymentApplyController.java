package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyUpdateReqVO;
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
 * 财务管理 - 付款申请 控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 付款申请")
@RestController
@RequestMapping("/lunaerp/finance/payment-apply")
@Validated
public class PaymentApplyController {

    @Resource
    private PaymentApplyService paymentApplyService;

    @PostMapping("/create")
    @Operation(summary = "创建付款申请单")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Long> createPaymentApply(@Valid @RequestBody PaymentApplyCreateReqVO createReqVO) {
        return success(paymentApplyService.createPaymentApply(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改付款申请单（仅 DRAFT/PENDING/REJECTED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> updatePaymentApply(@Valid @RequestBody PaymentApplyUpdateReqVO updateReqVO) {
        paymentApplyService.updatePaymentApply(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除付款申请单（仅 DRAFT/PENDING 软删进回收站）")
    @Parameter(name = "id", description = "申请单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> deletePaymentApply(@RequestParam("id") Long id) {
        paymentApplyService.deletePaymentApply(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款申请单详情")
    @Parameter(name = "id", description = "申请单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PaymentApplyRespVO> getPaymentApply(@RequestParam("id") Long id) {
        return success(paymentApplyService.getPaymentApply(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得付款申请单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<PaymentApplyRespVO>> getPaymentApplyPage(@Valid PaymentApplyPageReqVO pageReqVO) {
        return success(paymentApplyService.getPaymentApplyPage(pageReqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交付款申请单审批（02号审批引擎）")
    @Parameter(name = "id", description = "申请单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> submitPaymentApply(@RequestParam("id") Long id) {
        paymentApplyService.submitPaymentApply(id);
        return success(true);
    }

    @PostMapping("/cancel-approve")
    @Operation(summary = "撤回付款申请单审批（仅 APPROVING 态）")
    @Parameter(name = "id", description = "申请单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:apply')")
    public CommonResult<Boolean> cancelApprove(@RequestParam("id") Long id) {
        paymentApplyService.cancelApprove(id);
        return success(true);
    }

    @GetMapping("/prepay-balance")
    @Operation(summary = "获得供应商实时预付余额")
    @Parameter(name = "supplierId", description = "供应商ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<BigDecimal> getPrepayBalance(@RequestParam("supplierId") Long supplierId) {
        return success(paymentApplyService.getPrepayBalance(supplierId));
    }

}

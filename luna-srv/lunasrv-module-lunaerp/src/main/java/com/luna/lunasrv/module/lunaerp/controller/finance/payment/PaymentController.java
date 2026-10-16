package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.FinanceAccountSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 收付款（IN 侧收款草稿/审批/核销 + OUT 侧付款登记）控制器
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

    // ==================== IN 侧：客户收款单 ====================

    @PostMapping("/create")
    @Operation(summary = "创建收款单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:create')")
    public CommonResult<Long> createReceipt(@Valid @RequestBody ReceiptSaveReqVO reqVO) {
        return success(paymentService.createReceipt(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收款单草稿（仅 PENDING/REJECTED 可改）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:update')")
    public CommonResult<Boolean> updateReceipt(@Valid @RequestBody ReceiptUpdateReqVO reqVO) {
        paymentService.updateReceipt(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收款单草稿（仅 PENDING 可删）")
    @Parameter(name = "id", description = "收款单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:delete')")
    public CommonResult<Boolean> deleteReceipt(@RequestParam("id") Long id) {
        paymentService.deleteReceipt(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交收款单审批")
    @Parameter(name = "id", description = "收款单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:create')")
    public CommonResult<Boolean> submitReceipt(@RequestParam("id") Long id) {
        paymentService.submitReceipt(id);
        return success(true);
    }

    @PostMapping("/allocate-preview")
    @Operation(summary = "按发货日 FIFO 自动分配试算（纯内存不落库）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<AllocatePreviewRespVO>> allocatePreview(@Valid @RequestBody AllocatePreviewReqVO reqVO) {
        return success(paymentService.allocatePreview(reqVO));
    }

    @GetMapping("/ar-balance")
    @Operation(summary = "获得未核销 AR 分录明细列表（选单与核销数据源）")
    @Parameters({
            @Parameter(name = "customerId", description = "客户ID", required = true, example = "100"),
            @Parameter(name = "currency", description = "币种", required = true, example = "RMB")
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<ArBalanceRespVO>> getArBalanceList(@RequestParam("customerId") Long customerId,
                                                               @RequestParam("currency") String currency) {
        return success(paymentService.getArBalanceList(customerId, currency));
    }

    @GetMapping("/receipt-get")
    @Operation(summary = "获得收款单详情（内嵌拟核销明细行）")
    @Parameter(name = "id", description = "收款单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<ReceiptRespVO> getReceipt(@RequestParam("id") Long id) {
        return success(paymentService.getReceipt(id));
    }

    @GetMapping("/receipt-page")
    @Operation(summary = "获得收款单分页列表（direction=IN）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<ReceiptRespVO>> getReceiptPage(@Valid ReceiptPageReqVO pageReqVO) {
        return success(paymentService.getReceiptPage(pageReqVO));
    }

    // ==================== OUT 侧：供应商付款单 ====================

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

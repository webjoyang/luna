package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.CustomerReceiptService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 收付款登记 Controller（共享 Controller）
 *
 * @author luna
 */
@Tag(name = "管理后台 - 收付款登记")
@RestController
@RequestMapping("/lunaerp/finance/payment")
@Validated
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Resource
    private CustomerReceiptService customerReceiptService;

    @Resource
    private AccountService accountService;

    @Resource
    private PaymentMapper paymentMapper;

    // ==================== OUT 付款侧 ====================

    @PostMapping("/register")
    @Operation(summary = "付款登记（OUT 侧）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Long> registerPayment(@Valid @RequestBody PaymentRegisterReqVO reqVO) {
        return success(paymentService.registerPayment(reqVO));
    }

    // ==================== IN 收款侧 ====================

    @PostMapping("/create")
    @Operation(summary = "创建客户收款单草稿（IN 侧）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Long> createReceipt(@Valid @RequestBody ReceiptSaveReqVO createReqVO) {
        return success(customerReceiptService.createReceipt(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户收款单草稿（IN 侧）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Boolean> updateReceipt(@Valid @RequestBody ReceiptSaveReqVO updateReqVO) {
        customerReceiptService.updateReceipt(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户收款单草稿（IN 侧）")
    @Parameter(name = "id", description = "收款单编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Boolean> deleteReceipt(@RequestParam("id") Long id) {
        customerReceiptService.deleteReceipt(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交客户收款单审批（IN 侧）")
    @Parameter(name = "id", description = "收款单编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Long> submitReceipt(@RequestParam("id") Long id) {
        return success(customerReceiptService.submitReceipt(id));
    }

    @PostMapping("/allocate-preview")
    @Operation(summary = "自动核销分配试算（按发货日 FIFO）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<AllocatePreviewRespVO> allocatePreview(@Valid @RequestBody AllocatePreviewReqVO reqVO) {
        return success(customerReceiptService.allocatePreview(reqVO));
    }

    @GetMapping("/ar-balance")
    @Operation(summary = "查询客户指定币种的未核销应收分录列表（三视角选单源数据）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<ArBalanceRespVO>> getArBalance(@RequestParam("customerId") Long customerId,
                                                            @RequestParam("currency") String currency) {
        return success(customerReceiptService.getArBalanceList(customerId, currency));
    }

    @GetMapping("/account/simple-list")
    @Operation(summary = "收款选资金账户下拉列表（仅 ENABLE 状态）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<AccountSimpleRespVO>> getAccountSimpleList() {
        List<AccountDO> list = accountService.getSimpleAccountList();
        return success(BeanUtils.toBean(list, AccountSimpleRespVO.class));
    }

    @PutMapping("/reverse")
    @Operation(summary = "客户收款单红冲（IN 侧）")
    @Parameter(name = "id", description = "收款单编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:reverse')")
    public CommonResult<Boolean> reverseReceipt(@RequestParam("id") Long id) {
        customerReceiptService.reverseReceipt(id);
        return success(true);
    }

    @PostMapping("/advance-writeoff")
    @Operation(summary = "客户预收转核销（IN 侧）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:writeoff')")
    public CommonResult<Boolean> advanceWriteoff(@Valid @RequestBody AdvanceWriteoffReqVO reqVO) {
        customerReceiptService.advanceWriteoff(reqVO);
        return success(true);
    }

    @GetMapping("/advance-balance")
    @Operation(summary = "查询预收款单剩余可用预收余额（IN 侧）")
    @Parameter(name = "paymentId", description = "收款单编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<BigDecimal> getAdvanceBalance(@RequestParam("paymentId") Long paymentId) {
        return success(customerReceiptService.getAdvanceBalance(paymentId));
    }

    // ==================== 共享分页与详情 ====================

    @GetMapping("/page")
    @Operation(summary = "获得收付款单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<PaymentRespVO>> getPaymentPage(@Valid PaymentPageReqVO pageReqVO) {
        if ("IN".equalsIgnoreCase(pageReqVO.getDirection())) {
            return success(customerReceiptService.getReceiptPage(pageReqVO));
        }
        return success(paymentService.getPaymentPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得收付款单详情")
    @Parameter(name = "id", description = "收付款单编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PaymentRespVO> getPayment(@RequestParam("id") Long id) {
        PaymentDO payment = paymentMapper.selectById(id);
        if (payment != null && "IN".equalsIgnoreCase(payment.getDirection())) {
            return success(customerReceiptService.getReceipt(id));
        }
        return success(paymentService.getPayment(id));
    }

}

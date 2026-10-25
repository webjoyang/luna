package com.luna.lunasrv.module.lunaerp.controller.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceApplicationService;
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
 * 财务管理 - 发票申请控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 发票申请")
@RestController
@RequestMapping("/lunaerp/finance/invoice")
@Validated
public class InvoiceApplicationController {

    @Resource
    private InvoiceApplicationService invoiceApplicationService;

    @PostMapping("/create")
    @Operation(summary = "创建发票申请（草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:create')")
    public CommonResult<Long> createInvoiceApplication(@Valid @RequestBody InvoiceApplicationCreateReqVO createReqVO) {
        return success(invoiceApplicationService.createInvoiceApplication(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得发票申请详情")
    @Parameter(name = "id", description = "发票申请ID", required = true, example = "10001")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<InvoiceApplicationDetailRespVO> getInvoiceApplication(@RequestParam("id") Long id) {
        return success(invoiceApplicationService.getInvoiceApplication(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发票申请分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<PageResult<InvoiceApplicationRespVO>> getInvoiceApplicationPage(@Valid InvoiceApplicationPageReqVO pageReqVO) {
        return success(invoiceApplicationService.getInvoiceApplicationPage(pageReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发票申请（仅 DRAFT 允许）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:update')")
    public CommonResult<Boolean> updateInvoiceApplication(@Valid @RequestBody InvoiceApplicationUpdateReqVO updateReqVO) {
        invoiceApplicationService.updateInvoiceApplication(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发票申请（仅 DRAFT 允许）")
    @Parameter(name = "id", description = "发票申请ID", required = true, example = "10001")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:delete')")
    public CommonResult<Boolean> deleteInvoiceApplication(@RequestParam("id") Long id) {
        invoiceApplicationService.deleteInvoiceApplication(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交发票申请（DRAFT -> SUBMITTED）")
    @Parameter(name = "id", description = "发票申请ID", required = true, example = "10001")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:update')")
    public CommonResult<Boolean> submitInvoiceApplication(@RequestParam("id") Long id) {
        invoiceApplicationService.submitInvoiceApplication(id);
        return success(true);
    }

    @PutMapping("/issue")
    @Operation(summary = "登记开票回填（SUBMITTED -> ISSUED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:issue')")
    public CommonResult<Boolean> issueInvoiceApplication(@Valid @RequestBody InvoiceApplicationIssueReqVO reqVO) {
        invoiceApplicationService.issueInvoiceApplication(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回发票申请（SUBMITTED -> REJECTED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:issue')")
    public CommonResult<Boolean> rejectInvoiceApplication(@Valid @RequestBody InvoiceApplicationRejectReqVO reqVO) {
        invoiceApplicationService.rejectInvoiceApplication(reqVO);
        return success(true);
    }

    @GetMapping("/candidate-shipments")
    @Operation(summary = "查询指定客户和币种的可开票候选发货单列表")
    @Parameters({
            @Parameter(name = "customerId", description = "客户ID", required = true, example = "1001"),
            @Parameter(name = "currency", description = "币种", required = true, example = "RMB")
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<List<InvoiceCandidateShipmentRespVO>> getCandidateShipments(@RequestParam("customerId") Long customerId,
                                                                                   @RequestParam("currency") String currency) {
        return success(invoiceApplicationService.getCandidateShipments(customerId, currency));
    }

}
